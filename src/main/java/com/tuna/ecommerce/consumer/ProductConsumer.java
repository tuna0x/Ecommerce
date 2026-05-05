package com.tuna.ecommerce.consumer;

import com.tuna.ecommerce.config.RabbitMQConfig;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductImage;
import com.tuna.ecommerce.domain.message.ProductImageMessage;
import com.tuna.ecommerce.repository.ProductImageRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.service.CloudinaryService;
import com.tuna.ecommerce.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductConsumer {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_IMAGE_QUEUE)
    @Transactional
    public void processProductImages(ProductImageMessage message) throws Exception {
        log.info("Received image upload task for product ID: {}", message.getProductId());

        Optional<Product> productOpt = productRepository.findById(message.getProductId());
        if (productOpt.isEmpty()) {
            log.error("Product not found with ID: {}", message.getProductId());
            cleanupTempFiles(message.getTempFilePaths());
            return;
        }

        Product product = productOpt.get();

        try {
            for (String filePath : message.getTempFilePaths()) {
                File file = new File(filePath);
                if (file.exists()) {
                    log.info("Uploading file to Cloudinary: {}", filePath);
                    Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);

                    ProductImage image = new ProductImage();
                    image.setImageUrl(uploadResult.get("secure_url").toString());
                    image.setPublicId(uploadResult.get("public_id").toString());
                    image.setProduct(product);

                    productImageRepository.save(image);
                    product.addImage(image);
                    log.info("Successfully uploaded and saved image: {}", image.getImageUrl());

                    // TỐI ƯU: Chỉ xóa file sau khi đã upload thành công
                    try {
                        Files.deleteIfExists(Paths.get(filePath));
                        log.info("Deleted temp file after success: {}", filePath);
                    } catch (Exception e) {
                        log.warn("Could not delete temp file: {}", filePath);
                    }
                } else {
                    log.warn("Temp file not found (possibly already uploaded in previous attempt): {}", filePath);
                }
            }

            productRepository.saveAndFlush(product);
            log.info("Updated product {} with new images", product.getId());

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // Notify UI via WebSocket
                        messagingTemplate.convertAndSend("/topic/product-updates",
                                "Ảnh của sản phẩm " + product.getName() + " đã được xử lý xong.");

                        // Also send a toast notification to admins
                        notificationService.sendNotificationToAdmins(
                                "Xử lý ảnh hoàn tất",
                                "Ảnh cho sản phẩm " + product.getName() + " đã được tải lên thành công.",
                                "SYSTEM");
                        log.info("Sent WebSocket/Notifications after commit for product: {}", product.getId());
                    }
                });
            } else {
                // Fallback for non-transactional contexts (though this method is
                // @Transactional)
                messagingTemplate.convertAndSend("/topic/product-updates",
                        "Ảnh của sản phẩm " + product.getName() + " đã được xử lý xong.");

                notificationService.sendNotificationToAdmins(
                        "Xử lý ảnh hoàn tất",
                        "Ảnh cho sản phẩm " + product.getName() + " đã được tải lên thành công.",
                        "SYSTEM");
            }

        } catch (Exception e) {
            log.error("Error processing images for product ID: {}. Retrying...", message.getProductId(), e);
            throw e; // Rethrow to trigger Spring AMQP retry policy
        }
    }

    private void cleanupTempFiles(java.util.List<String> filePaths) {
        for (String path : filePaths) {
            try {
                Files.deleteIfExists(Paths.get(path));
                log.info("Deleted temp file: {}", path);
            } catch (Exception e) {
                log.error("Failed to delete temp file: {}", path, e);
            }
        }
    }
}
