package in.onlinebank.backend.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@SuppressWarnings("deprecation")
@Configuration
@EnableResourceServer
public class OauthResourceServerConfig extends ResourceServerConfigurerAdapter
{
    @Override
    public void configure(HttpSecurity http) throws Exception
    {
        http
                .csrf() .disable()
                .authorizeRequests()
                .antMatchers("/onlinebaking/backend/public/**").authenticated()
                .antMatchers("/onlinebaking/backend/public/**").permitAll()
                .and()
                .formLogin().permitAll()
                .and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .and().cors();

    }

}

