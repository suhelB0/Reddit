package com.reddit.clone.controller;

import com.reddit.clone.dto.CommentDto;
import com.reddit.clone.dto.PostDto;
import com.reddit.clone.dto.VoteDto;
import com.reddit.clone.entity.Post;
import com.reddit.clone.entity.Comment;
import com.reddit.clone.service.CommentService;
import com.reddit.clone.service.CommunityService;
import com.reddit.clone.service.PostService;
import com.reddit.clone.service.VoteService;
import com.reddit.clone.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/posts")
@AllArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final CommunityService communityService;
    private final VoteService voteService;
    private final AuthService authService;

    @GetMapping
    public String listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "hot") String sort,
            Model model) {

        var currentUser= authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        var posts = sort.equals("new") ?
                postService.getNewPosts(pageable) :
                postService.getHotPosts(pageable);

        posts.forEach(post -> {
            VoteDto userVote = voteService.getUserVoteForPost(post.getId());
            if (userVote != null) {
                post.setVoteStatus(userVote.getVoteType().name());
            } else {
                post.setVoteStatus(null); // User hasn't voted
            }
        });

        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("joinedCommunities", communityService.getCommunitiesJoinedByUser(currentUser));

        return "post/list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/create")
    public String showCreateForm(
            @RequestParam(required = false) String community,
            Model model) {

        var currentUser=authService.getCurrentUser();
        PostDto postDto = new PostDto();
        if (community != null) {
            postDto.setCommunityName(community);
        }

        model.addAttribute("postDto", postDto);
//        model.addAttribute("communities", communityService.getAllCommunities());
        model.addAttribute("communities", communityService.getCommunitiesJoinedByUser(currentUser));
        model.addAttribute("currentUser",currentUser);
        return "post/create";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    public String createPost(
            @Valid @ModelAttribute PostDto postDto,
            BindingResult result,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Model model,
            RedirectAttributes redirectAttributes) {

        var currentUser=authService.getCurrentUser();
        System.out.println(postDto.getTitle());
    System.out.println(postDto.getContent());
    if (result.hasErrors()) {
        result.getAllErrors().forEach(error -> {
            System.err.println("Field: " + (error.getCodes() != null ? error.getCodes()[0] : "N/A"));
            System.err.println("Error Message: " + error.getDefaultMessage());
        });
        model.addAttribute("communities", communityService.getAllCommunities());
        model.addAttribute("joinedCommunities", communityService.getCommunitiesJoinedByUser(currentUser));
        return "post/create";
    }

    try {
        if (files == null) {
            files = new MultipartFile[0];  // avoid null pointer
        }
        PostDto savedPost = postService.savePostWithFiles(postDto, files);
        redirectAttributes.addFlashAttribute("success", "Post created successfully!");
        return "redirect:/posts/" + savedPost.getId();
    } catch (Exception e) {
        result.rejectValue("communityName", "error.postDto", e.getMessage());
        model.addAttribute("communities", communityService.getAllCommunities());
        return "post/create";
    }
}

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        var currentUser=authService.getCurrentUser();
        PostDto post = postService.getPostById(id);
        List<Comment> comments = commentService.getCommentsByPost(id);

        VoteDto userVote = voteService.getUserVoteForPost(post.getId());
        if (userVote != null) {
            post.setVoteStatus(userVote.getVoteType().name());
        } else {
            post.setVoteStatus(null); // User hasn't voted
        }

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentDto", new CommentDto());
//        model.addAttribute("joinedCommunities", communityService.getCommunitiesJoinedByUser(currentUser));

        model.addAttribute("communities", communityService.getCommunitiesJoinedByUser(currentUser));
        model.addAttribute("currentUser",currentUser);
        // Add this to fix the voteDto binding error
        model.addAttribute("voteDto", new VoteDto());
        model.addAttribute("existingFiles", postService.getPostFiles(id)); // Get existing files

        return "post/view";
    }

    @PreAuthorize("@postService.isOwner(#id)")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            PostDto post = postService.getPostById(id);
            model.addAttribute("postDto", post);
            model.addAttribute("communities", communityService.getAllCommunities());
            model.addAttribute("existingFiles", postService.getPostFiles(id)); // Get existing files
            return "post/edit";
        } catch (Exception e) {
            return "redirect:/posts?error=Post not found";
        }
    }

    @PreAuthorize("@postService.isOwner(#id)")
    @PostMapping("/{id}/edit")
    public String updatePost(
            @PathVariable Long id,
            @Valid @ModelAttribute PostDto postDto,
            BindingResult result,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "deleteFileIds", required = false) List<Long> deleteFileIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("communities", communityService.getAllCommunities());
            model.addAttribute("existingFiles", postService.getPostFiles(id));
            return "post/edit";
        }

        try {
            postService.updatePostWithFiles(id, postDto, files, deleteFileIds);
            redirectAttributes.addFlashAttribute("success", "Post updated successfully!");
            return "redirect:/posts/" + id;
        } catch (Exception e) {
            result.rejectValue("title", "error.postDto", e.getMessage());
            model.addAttribute("communities", communityService.getAllCommunities());
            model.addAttribute("existingFiles", postService.getPostFiles(id));
            return "post/edit";
        }
    }

    @PreAuthorize("@postService.isOwner(#id)")
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            postService.deletePost(id);
            redirectAttributes.addFlashAttribute("success", "Post deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete post: " + e.getMessage());
        }
        return "redirect:/posts";
    }

}

