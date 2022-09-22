package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.MyUserDetails;
import in.onlinebank.backend.entity.UserEntity;
import in.onlinebank.backend.repository.UserRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(MyUserDetailsService.class);

    @Autowired
    UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        Optional<UserEntity> user = userRepo.findById(Long.parseLong(userId));
        return  user.map(MyUserDetails::new).orElse(null);

    }

    public long getLoggedInUserId() {
        long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        LOGGER.debug(userId);
        return userId;
    }

    public String getLoggedInUserRole() {
        List<GrantedAuthority> list = (List<GrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().collect(Collectors.toList());
        return String.valueOf(list.get(0));
    }

    public boolean ifUserAlreadyLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            return false;
        }
        return authentication.isAuthenticated();
    }
}
