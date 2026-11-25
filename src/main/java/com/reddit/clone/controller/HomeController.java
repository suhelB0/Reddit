package com.reddit.clone.controller;

import com.reddit.clone.dto.PostDto;
import com.reddit.clone.dto.VoteDto;
import com.reddit.clone.entity.Post;
import com.reddit.clone.mapper.PostMapper;
import com.reddit.clone.service.CommunityService;
import com.reddit.clone.service.PostService;
import com.reddit.clone.service.VoteService;
import com.reddit.clone.service.NotificationService;
import com.reddit.clone.service.auth.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@AllArgsConstructor
public class HomeController {

    private final PostService postService;
    private final CommunityService communityService;
    private final PostMapper postMapper;
    private final AuthService authService;
    private final VoteService voteService;
    private final NotificationService notificationService;

    @GetMapping({"/", "/home"})
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "new") String sort,
            Model model) {
        System.out.println("HomeController home method called");
        var currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        var posts = sort.equals("new") ?
                postService.getNewPosts(pageable) :
                postService.getHotPosts(pageable);

        var communities = communityService.getAllCommunities(PageRequest.of(0, 5));

        // Add vote status for each post
        posts.forEach(post -> {
            VoteDto userVote = voteService.getUserVoteForPost(post.getId());
            if (userVote != null) {
                post.setVoteStatus(userVote.getVoteType().name());
            } else {
                post.setVoteStatus(null);
            }
        });

        // Add notifications
        if (currentUser != null) {
            var notifications = notificationService.getNotifications(currentUser);
            System.out.println("Notifications: found " + notifications);
            var unreadNotifications = notificationService.getUnreadNotifications(currentUser);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadNotifications.size());
        } else {
            System.out.println("No current user");
            model.addAttribute("notifications", List.of());
            model.addAttribute("unreadCount", 0);
        }

        model.addAttribute("joinedCommunities", communityService.getCommunitiesJoinedByUser(currentUser));
        model.addAttribute("posts", posts);
        model.addAttribute("communities", communities);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("currentUser", currentUser);

        return "index";
    }

    @GetMapping("/search")
    public String searchPosts(@RequestParam("query") String query,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {

        var currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        // Get search results
        Page<Post> postEntities = postService.searchPostsByAll(query, pageable);
        Page<PostDto> postDtos = postEntities.map(postMapper::mapEntityToDto);

        var communities = communityService.getAllCommunities(PageRequest.of(0, 5));

        // Add notifications
        if (currentUser != null) {
            var notifications = notificationService.getNotifications(currentUser);
            var unreadNotifications = notificationService.getUnreadNotifications(currentUser);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadNotifications.size());
        } else {
            model.addAttribute("notifications", List.of());
            model.addAttribute("unreadCount", 0);
        }

        model.addAttribute("posts", postDtos);
        model.addAttribute("joinedCommunities", communityService.getCommunitiesJoinedByUser(currentUser));
        model.addAttribute("communities", communities);
        model.addAttribute("currentPage", postDtos.getNumber());
        model.addAttribute("totalPages", postDtos.getTotalPages());
        model.addAttribute("query", query);
        model.addAttribute("currentUser", currentUser);

        return "index";
    }
}
