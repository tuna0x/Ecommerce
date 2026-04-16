package com.tuna.ecommerce.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.Blog;
import com.tuna.ecommerce.repository.BlogRepository;

@Service
public class BlogService {
    private final BlogRepository blogRepository;
    private final CloudinaryService cloudinaryService;

    public BlogService(BlogRepository blogRepository, CloudinaryService cloudinaryService) {
        this.blogRepository = blogRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public Blog handleCreateBlog(Blog blog, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            var uploadResult = this.cloudinaryService.uploadFile(file);
            blog.setImage((String) uploadResult.get("url"));
        }
        return this.blogRepository.save(blog);
    }

    public Blog handleUpdateBlog(Blog blog, MultipartFile file) throws IOException {
        Optional<Blog> blogOptional = this.blogRepository.findById(blog.getId());
        if (blogOptional.isPresent()) {
            Blog currentBlog = blogOptional.get();
            if (file != null && !file.isEmpty()) {
                var uploadResult = this.cloudinaryService.uploadFile(file);
                currentBlog.setImage((String) uploadResult.get("url"));
            }
            currentBlog.setTitle(blog.getTitle());
            currentBlog.setExcerpt(blog.getExcerpt());
            currentBlog.setContent(blog.getContent());
            currentBlog.setCategory(blog.getCategory());
            currentBlog.setAuthor(blog.getAuthor());
            currentBlog.setReadTime(blog.getReadTime());
            return this.blogRepository.save(currentBlog);
        }
        return null;
    }

    public void handleDeleteBlog(long id) {
        this.blogRepository.deleteById(id);
    }

    public Optional<Blog> fetchBlogById(long id) {
        return this.blogRepository.findById(id);
    }

    public Page<Blog> fetchAllBlogs(Specification<Blog> spec, Pageable pageable) {
        return this.blogRepository.findAll(spec, pageable);
    }
}
