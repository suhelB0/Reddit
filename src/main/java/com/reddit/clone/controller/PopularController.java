package com.reddit.clone.controller;

import com.reddit.clone.dto.VoteDto;
import com.reddit.clone.entity.Post;
import com.reddit.clone.mapper.PostMapper;
import com.reddit.clone.service.CommunityService;
import com.reddit.clone.service.PostService;
import com.reddit.clone.service.VoteService;
import com.reddit.clone.service.auth.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class PopularController {
    private final PostService postService;
    private final CommunityService communityService;
    private final PostMapper postMapper;
    private final AuthService authService;
    private final VoteService voteService;


    @GetMapping("/popular")
    public String getPopularPosts(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model){
        var currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        var popularPosts = postService.findPostsByPopularity(pageable);

        popularPosts.forEach(post -> {
            VoteDto userVote = voteService.getUserVoteForPost(post.getId());
            if (userVote != null) {
                post.setVoteStatus(userVote.getVoteType().name());
            } else {
                post.setVoteStatus(null); // User hasn't voted
            }
        });
        model.addAttribute("posts",popularPosts);
        model.addAttribute("currentPage", popularPosts.getNumber());
        model.addAttribute("totalPages", popularPosts.getTotalPages());
        model.addAttribute("joinedCommunities", communityService.getCommunitiesJoinedByUser(currentUser));
        return "post/popularList";
    }
}
