package com.tuna.ecommerce.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Inventory;
import com.tuna.ecommerce.domain.InventoryLog;
import com.tuna.ecommerce.repository.InventoryLogRepository;
import com.tuna.ecommerce.repository.InventoryRepository;
import com.tuna.ecommerce.ultil.constant.InventoryLogType;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisInventoryReservationService {

    private static final String KEY_PREFIX = "inventory:reservation:";
    private static final String DIRTY_SET_KEY = KEY_PREFIX + "dirty";

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            local quantity = tonumber(ARGV[1])
            if stock < quantity then
                return -1
            end
            redis.call('DECRBY', KEYS[1], quantity)
            redis.call('INCRBY', KEYS[2], quantity)
            return stock - quantity
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            local quantity = tonumber(ARGV[1])
            local reserved = tonumber(redis.call('GET', KEYS[2]) or '0')
            if reserved < quantity then
                quantity = reserved
            end
            if quantity <= 0 then
                return 0
            end
            redis.call('INCRBY', KEYS[1], quantity)
            redis.call('DECRBY', KEYS[2], quantity)
            return quantity
            """, Long.class);

    private static final DefaultRedisScript<Long> ENQUEUE_PENDING_SCRIPT = new DefaultRedisScript<>("""
            local quantity = tonumber(ARGV[1])
            if quantity == 0 then
                return 0
            end
            local pending = redis.call('INCRBY', KEYS[1], quantity)
            if pending == 0 then
                redis.call('SREM', KEYS[2], ARGV[2])
            else
                redis.call('SADD', KEYS[2], ARGV[2])
            end
            return quantity
            """, Long.class);

    private static final DefaultRedisScript<Long> CLAIM_PENDING_SCRIPT = new DefaultRedisScript<>("""
            local pending = tonumber(redis.call('GET', KEYS[1]) or '0')
            if pending == 0 then
                redis.call('SREM', KEYS[2], ARGV[1])
                return 0
            end
            redis.call('SET', KEYS[1], 0)
            redis.call('SREM', KEYS[2], ARGV[1])
            return pending
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Value("${inventory.redis-reservation.enabled:false}")
    private boolean enabled;

    @Value("${inventory.redis-reservation.flush-batch-size:200}")
    private int flushBatchSize;

    public boolean isEnabled() {
        return enabled;
    }

    public int reserve(Inventory inventory, int quantity) throws IdInvalidException {
        if (inventory == null || inventory.getId() == null) {
            throw new IdInvalidException("Inventory khong hop le.");
        }

        Long inventoryId = inventory.getId();
        initializeKeysIfAbsent(inventory);

        Long remaining = redisTemplate.execute(
                RESERVE_SCRIPT,
                java.util.List.of(stockKey(inventoryId), reservedKey(inventoryId)),
                String.valueOf(quantity));

        if (remaining == null || remaining < 0) {
            throw new IdInvalidException("Chi con " + getRedisStock(inventoryId) + " san pham trong kho.");
        }

        return remaining.intValue();
    }

    public void enqueueDatabaseFlush(Long inventoryId, int quantity) {
        redisTemplate.execute(
                ENQUEUE_PENDING_SCRIPT,
                java.util.List.of(pendingKey(inventoryId), DIRTY_SET_KEY),
                String.valueOf(quantity),
                String.valueOf(inventoryId));
    }

    public int releaseReservation(Long inventoryId, int quantity) {
        Long released = redisTemplate.execute(
                RELEASE_SCRIPT,
                java.util.List.of(stockKey(inventoryId), reservedKey(inventoryId)),
                String.valueOf(quantity));
        return released != null ? released.intValue() : 0;
    }

    public boolean hasReservationState(Long inventoryId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(stockKey(inventoryId)))
                && Boolean.TRUE.equals(redisTemplate.hasKey(reservedKey(inventoryId)));
    }

    public int getPendingFlushQuantity(Long inventoryId) {
        String value = redisTemplate.opsForValue().get(pendingKey(inventoryId));
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void initializeKeysIfAbsent(Inventory inventory) {
        Long inventoryId = inventory.getId();
        redisTemplate.opsForValue().setIfAbsent(stockKey(inventoryId), String.valueOf(inventory.getStock()));
        redisTemplate.opsForValue().setIfAbsent(reservedKey(inventoryId), String.valueOf(inventory.getReservedStock()));
        redisTemplate.opsForValue().setIfAbsent(pendingKey(inventoryId), "0");
    }

    private int getRedisStock(Long inventoryId) {
        String value = redisTemplate.opsForValue().get(stockKey(inventoryId));
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Scheduled(fixedDelayString = "${inventory.redis-reservation.flush-delay-ms:1000}")
    @Transactional
    public void flushPendingReservations() {
        if (!enabled) {
            return;
        }

        Set<String> dirtyInventoryIds = redisTemplate.opsForSet().members(DIRTY_SET_KEY);
        if (dirtyInventoryIds == null || dirtyInventoryIds.isEmpty()) {
            return;
        }

        int processed = 0;
        for (String inventoryIdValue : dirtyInventoryIds) {
            if (processed >= flushBatchSize) {
                return;
            }

            Long inventoryId;
            try {
                inventoryId = Long.parseLong(inventoryIdValue);
            } catch (NumberFormatException e) {
                redisTemplate.opsForSet().remove(DIRTY_SET_KEY, inventoryIdValue);
                continue;
            }

            Long quantity = redisTemplate.execute(
                    CLAIM_PENDING_SCRIPT,
                    java.util.List.of(pendingKey(inventoryId), DIRTY_SET_KEY),
                    inventoryIdValue);

            if (quantity == null || quantity == 0) {
                continue;
            }

            int updatedRows = inventoryRepository.applyReservationDelta(inventoryId, quantity.intValue());
            if (updatedRows == 0) {
                requeuePending(inventoryId, quantity);
                log.warn("Failed to flush Redis inventory reservation. inventoryId={}, quantity={}", inventoryId, quantity);
                continue;
            }

            Inventory inventoryRef = inventoryRepository.getReferenceById(inventoryId);
            InventoryLog logEntry = new InventoryLog();
            logEntry.setInventory(inventoryRef);
            logEntry.setQuantityChange(-quantity.intValue());
            logEntry.setType(quantity > 0 ? InventoryLogType.RESERVE : InventoryLogType.RELEASE);
            logEntry.setNote(quantity > 0
                    ? "Dong bo Redis reservation theo batch"
                    : "Hoan Redis reservation theo batch");
            inventoryLogRepository.save(logEntry);
            processed++;
        }
    }

    private void requeuePending(Long inventoryId, Long quantity) {
        redisTemplate.opsForValue().increment(pendingKey(inventoryId), quantity);
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, String.valueOf(inventoryId));
    }

    private String stockKey(Long inventoryId) {
        return KEY_PREFIX + "stock:" + inventoryId;
    }

    private String reservedKey(Long inventoryId) {
        return KEY_PREFIX + "reserved:" + inventoryId;
    }

    private String pendingKey(Long inventoryId) {
        return KEY_PREFIX + "pending:" + inventoryId;
    }
}
