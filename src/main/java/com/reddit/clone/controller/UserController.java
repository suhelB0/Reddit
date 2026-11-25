package com.reddit.clone.controller;

import com.reddit.clone.dto.UserDto;
import com.reddit.clone.dto.UserProfileDto;
import com.reddit.clone.dto.UserViewDto;
import com.reddit.clone.entity.User;
import com.reddit.clone.entity.UserProfile;
import com.reddit.clone.mapper.UserProfileMapper;
import com.reddit.clone.service.UserProfileService;
import com.reddit.clone.service.UserService;
import com.reddit.clone.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileService userProfileService;
    private final AuthService authService;

    public UserController(UserService userService, UserProfileMapper userProfileMapper, UserProfileService userProfileService, AuthService authService) {
        this.userService = userService;
        this.userProfileMapper = userProfileMapper;
        this.userProfileService = userProfileService;
        this.authService = authService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "user/create";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        log.debug("User Data is {}", user);
        User newUser = userService.register(user);
        log.debug("It is in the Post Mapping {}", newUser);
        return "redirect:/login";
    }

    @GetMapping("/user/{username}")
    public String viewUserProfile(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);
        log.debug("In the Userview Profile {}", userView.getIsOwnProfile());
        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "overview");

        return "user/view";
    }

    @GetMapping("/user/{username}/posts")
    public String viewUserPosts(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);

        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "posts");

        return "user/view";
    }

    @GetMapping("/user/{username}/comments")
    public String viewUserComments(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);

        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "comments");

        return "user/view";
    }

    @GetMapping("/user/{username}/upvoteposts")
    public String viewUserUpvotePosts(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);

        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "upvoteposts");

        return "user/view";
    }

    @GetMapping("/user/{username}/downposts")
    public String viewUserDownPosts(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);

        log.debug("User posts {}", userView);

        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "downposts");

        return "user/view";
    }

    @GetMapping("/user/{username}/followers")
    public String viewUserFollowers(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);

        log.debug("User followers {}", userView);

        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "followers");

        return "user/view";
    }

    @GetMapping("/user/{username}/following")
    public String viewUserFollowing(@PathVariable String username, Model model) {
        UserViewDto userView = userService.getUserView(username);

        log.debug("User following {}", userView);

        model.addAttribute("userView", userView);
        model.addAttribute("activeTab", "following");

        return "user/view";
    }

    @PreAuthorize("@userService.isOwner(#username)")
    @GetMapping("/user/{username}/edit")
    public String showEditProfile(@PathVariable String username,
                                  Model model) {

        User user = userService.findByUsername(username);
        UserProfile userProfile = user.getProfile();

        // Write this in the UserProfile Mapper
        UserProfileDto form = userProfileMapper.mapEntityToDto(userProfile);

        model.addAttribute("username", username);
        model.addAttribute("userUpdateForm", form);

        log.debug("Form {}",form);

        return "user/edit";
    }

    // We can use put method by using the hidden http Layer, As of now keep it simple
    @PreAuthorize("@userService.isOwner(#username)")
    @PostMapping("/user/{username}/edit")
    public String updateProfile(@PathVariable String username,
                                @ModelAttribute("userUpdateForm") UserProfileDto form) {

        User user = userService.findByUsername(username);
        UserProfile userProfile = user.getProfile() != null ? user.getProfile() : new UserProfile();
        userProfile.setUser(user);
        userProfile = userProfileMapper.mapDtoToEntity(form, userProfile);

        userProfileService.saveUserProfile(userProfile);

        return "redirect:/user/" + username;
    }


    @GetMapping("/user/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public String followUser(@PathVariable String username) {
        User loggedInUser = authService.getCurrentUser();
        User targetUser = userService.findByUsername(username);

        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if (loggedInUser.getUsername().equals(username)) {
            return "redirect:/user/" + username;
        }

        userService.followUser(loggedInUser, targetUser);
        return "redirect:/user/" + username;
    }

    @GetMapping("/user/{username}/unfollow")
    @PreAuthorize("isAuthenticated()")
    public String unfollowUser(@PathVariable String username) {
        User loggedInUser = authService.getCurrentUser();
        User targetUser = userService.findByUsername(username);

        if (loggedInUser == null) {
            return "redirect:/login";
        }

        System.out.println("User Name" + username + "Target user" + targetUser);

        userService.unfollowUser(loggedInUser, targetUser);
        return "redirect:/user/" + username;

//        return "test";
    }

}

