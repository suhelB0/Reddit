package com.reddit.clone.service;

import com.reddit.clone.dto.PostDto;
import com.reddit.clone.dto.PostFileDto;
import com.reddit.clone.entity.Community;
import com.reddit.clone.entity.Post;
//import com.reddit.clone.entity.PostDocument;
import com.reddit.clone.entity.PostFile;
import com.reddit.clone.entity.User;
import com.reddit.clone.exception.CommunityNotFoundException;
import com.reddit.clone.exception.PostNotFoundException;
import com.reddit.clone.mapper.PostMapper;
import com.reddit.clone.repository.CommunityRepository;
import com.reddit.clone.repository.PostFileRepository;
import com.reddit.clone.repository.PostRepository;
//import com.reddit.clone.repository.PostSearchRepository;
import com.reddit.clone.service.auth.AuthService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;
    private final PostMapper postMapper;
    private final CloudinaryService cloudinaryService;
    private final PostFileRepository postFileRepository;
    private final AuthService authService;


    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public PostDto savePostWithFiles(PostDto postDto, MultipartFile[] files) {
        System.out.println("inside savepostWithfiles method");
        log.info("Saving post: {}", postDto);
        Community community = communityRepository.findByName(postDto.getCommunityName())
                .orElseThrow(() -> new CommunityNotFoundException("Community not found"));

        Post post = postMapper.mapDtoToEntity(postDto);
        post.setCommunity(community);

        User user = authService.getCurrentUser();
        post.setUser(user);

        List<PostFile> postFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String url = cloudinaryService.uploadFile(file);
                System.out.println("Uploaded file URL: " + url);
                log.info("Uploaded file to Cloudinary: {}", url);

                PostFile postFile = PostFile.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileUrl(url)
                        .post(post)
                        .build();

                postFiles.add(postFile);
            }
        }

        post.setFiles(postFiles);

        try {
            Post saved = postRepository.save(post);
            System.out.println("Saved post ID: " + saved.getId());
            log.info("Saved post with id: {}", saved.getId());
            return postMapper.mapEntityToDto(saved);
        } catch (Exception e) {
            log.error("Error saving post", e);
            throw e;
        }
    }

    @Cacheable(value = "postCache", key = "#id")
    @Transactional(readOnly = true)
    public PostDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + id));
        return postMapper.mapEntityToDto(post);
    }

    @Cacheable(value = "communityPostsCache", key = "#communityId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<PostDto> getPostsByCommunity(Long communityId, Pageable pageable) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found with ID: " + communityId));

        return postRepository.findByCommunity(community, pageable)
                .map(postMapper::mapEntityToDto);
    }

    @Cacheable(value = "hotPostsCache", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<PostDto> getHotPosts(Pageable pageable) {
        return postRepository.findHotPosts(pageable)
                .map(postMapper::mapEntityToDto);
    }

    @Cacheable(value = "newPostsCache", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<PostDto> getNewPosts(Pageable pageable) {
        return postRepository.findNewPosts(pageable)
                .map(postMapper::mapEntityToDto);
    }

    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException("Post not found with ID: " + id);
        }
        postRepository.deleteById(id);
        log.info("Post deleted with ID: {}", id);
    }

    @Cacheable(value = "searchPostsCache", key = "#text + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Post> searchPosts(String text, Pageable pageable) {
        return postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(text, text, pageable);
    }

    public Page<Post> searchPostsByAll(String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchByTitleContentOrCommunity(keyword, pageable);
        return posts;
    }

    // Add this method to get existing files
    @Cacheable(value = "postFilesCache", key = "#postId")
    @Transactional(readOnly = true)
    public List<PostFileDto> getPostFiles(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        if (post.getFiles() == null || post.getFiles().isEmpty()) {
            return Collections.emptyList();
        }

        return post.getFiles().stream()
                .map(file -> PostFileDto.builder()
                        .id(file.getId())
                        .fileName(file.getFileName())
                        .fileType(file.getFileType())
                        .fileUrl(file.getFileUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // Update this method to handle file uploads
    @CacheEvict(value = {"communityCache", "allCommunitiesCache", "searchCommunitiesCache",
            "postCache", "postFilesCache", "communityPostsCache", "hotPostsCache", "newPostsCache", "userCache", "userViewCache"
    }, allEntries = true)
    public PostDto updatePostWithFiles(Long id, PostDto postDto, MultipartFile[] files, List<Long> deleteFileIds) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + id));

        // Update basic fields
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setPostType(postDto.getPostType());

        // Update community if changed
        if (postDto.getCommunityName() != null &&
                !postDto.getCommunityName().equals(post.getCommunity().getName())) {
            Community newCommunity = communityRepository.findByName(postDto.getCommunityName())
                    .orElseThrow(() -> new CommunityNotFoundException(
                            "Community not found: " + postDto.getCommunityName()));
            post.setCommunity(newCommunity);
        }

        // Delete marked files
        if (deleteFileIds != null && !deleteFileIds.isEmpty()) {
            postFileRepository.deleteByIdIn(deleteFileIds);
            log.info("Deleted {} files from database", deleteFileIds.size());

            // Also remove from the post's file list to keep in-memory state consistent
            post.getFiles().removeIf(file -> deleteFileIds.contains(file.getId()));
        }

        // Add new files
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    log.info("Uploaded new file to Cloudinary: {}", url);

                    PostFile postFile = PostFile.builder()
                            .fileName(file.getOriginalFilename())
                            .fileType(file.getContentType())
                            .fileUrl(url)
                            .post(post)
                            .build();

                    post.getFiles().add(postFile);
                }
            }
        }

        Post updatedPost = postRepository.save(post);
        log.info("Post updated: {}", updatedPost.getTitle());
        return postMapper.mapEntityToDto(updatedPost);
    }

    @Cacheable(value = "popularPostsCache", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PostDto> findPostsByPopularity(Pageable pageable){
        System.out.println("Fetching from findPostsByPopularity...");
        return postRepository.findPopularPosts(pageable).map(postMapper::mapEntityToDto);
    }

    public boolean isOwner(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID: " + postId));

        if (post == null) return false;
        return post.getUser().getId().equals(currentUser.getId());
    }

}

