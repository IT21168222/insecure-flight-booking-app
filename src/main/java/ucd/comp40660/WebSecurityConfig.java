package ucd.comp40660;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ucd.comp40660.filter.JWTAuthenticationFilter;
import ucd.comp40660.filter.JWTAuthorisationFilter;
import ucd.comp40660.service.UserDetailsServiceImplementation;

import java.util.Arrays;

import static ucd.comp40660.filter.SecurityConstants.COOKIE_NAME;

@Configuration
@EnableWebSecurity
@EnableEncryptableProperties
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{
    @Qualifier("userDetailsServiceImplementation")
    @Autowired
    private UserDetailsService userDetailsService;

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception{
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception{
        auth.authenticationProvider(authenticationProvider());
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        bCryptPasswordEncoder = new BCryptPasswordEncoder(12);
        authProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return authProvider;
    }

    public WebSecurityConfig(UserDetailsServiceImplementation userDetailsService, BCryptPasswordEncoder bCryptPasswordEncoder){
        this.userDetailsService = userDetailsService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception{

        // Enable Cross Origin RequestS (cors) and disable Spring Boot CSRF protection
        // CSRF protection is automatically enabled by Spring Security to create a stateful session, while we are using stateless session here
        // thus it has to be disabled here
        http.cors().and().csrf().disable()
//                .requiresChannel().anyRequest().requiresSecure()  // Require HTTPS Requests
//                .and()
                .authorizeRequests()
                .antMatchers("/error","/resources/**","/img/**", "/css/**","/js/**", "/login","/register","/").permitAll()
                .antMatchers("/user").access("hasAnyAuthority('ADMIN','USER')")
                .antMatchers("/editProfile").access("hasAuthority('USER')")
                .antMatchers("/admin", "/adminRegister", "/users").access("hasAuthority('ADMIN')")
                .anyRequest().authenticated()   // Authenticate all requests, with exception URL regexes mentioned above
                .and()
                .formLogin()
                .defaultSuccessUrl("/", true)
                //.successHandler(authenticationSuccessHandler)
                .loginPage("/login")            // Specify URL for login
                .permitAll()
                .successForwardUrl("/")
                .and()
                .logout()
                .logoutUrl("/logout")           // Specify URL for logout
                .deleteCookies(COOKIE_NAME)     // Delete the cookie containing the JWT after logout
                .permitAll()
                .and()
                // Filtering by intercepting incoming requests and execute predefined methods
                .addFilter(new JWTAuthenticationFilter(authenticationManager()))    // filter support authentication
                .addFilter(new JWTAuthorisationFilter(authenticationManager()))     // filter support authorization
                // Enforce stateless sessions : this disables session creation 0on Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        configuration.setAllowedOrigins(Arrays.asList("localhost"));
        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        return source;
    }

}