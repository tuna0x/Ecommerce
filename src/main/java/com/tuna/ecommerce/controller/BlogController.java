package com.tuna.ecommerce.controller;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.Blog;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.BlogService;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

@RestController
@RequestMapping("/api/v1")
public class BlogController {
    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping("/blogs")
    public ResponseEntity<Blog> createNewBlog(
            @RequestPart("data") Blog blog,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.blogService.handleCreateBlog(blog, file));
    }

    @PutMapping("/blogs")
    public ResponseEntity<Blog> updateBlog(
            @RequestPart("data") Blog blog,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return ResponseEntity.ok(this.blogService.handleUpdateBlog(blog, file));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable("id") long id) {
        this.blogService.handleDeleteBlog(id);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/blogs/{id}")
    public ResponseEntity<Blog> getBlogById(@PathVariable("id") long id) throws IdInvalidException {
        Blog blog = this.blogService.fetchBlogById(id).orElse(null);
        if (blog == null) {
            throw new IdInvalidException("Blog với id = " + id + " không tồn tại");
        }
        return ResponseEntity.ok(blog);
    }

    @GetMapping("/blogs")
    public ResponseEntity<ResultPaginationDTO> getAllBlogs(
            @Filter Specification<Blog> spec,
            Pageable pageable) {
        Page<Blog> pageBlogs = this.blogService.fetchAllBlogs(spec, pageable);
        ResultPaginationDTO res = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageBlogs.getTotalPages());
        mt.setTotal(pageBlogs.getTotalElements());

        res.setMeta(mt);
        res.setResult(pageBlogs.getContent());

        return ResponseEntity.ok(res);
    }
}
