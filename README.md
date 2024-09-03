public class IpRestrictionFilter extends OncePerRequestFilter {
private static final List<String> ALLOWED_IPS = List.of("192.168.1.100", "203.0.113.1");  // Replace with your allowed IPs
Key Points in the IpRestrictionFilter:
OncePerRequestFilter: This class ensures that the filter is applied once per request, suitable for implementing security filters.
doFilterInternal Method: Contains the core logic for checking the IP address and deciding whether to allow or reject the request.
Allowed IPs List (ALLOWED_IPS): This list contains the IP addresses that are allowed to access the application. You can customize this list as needed.
Step 2: Register the IpRestrictionFilter in Spring Security Configuration
To activate the custom filter, you need to register it in your Spring Security configuration class (SecurityConfig).

    @Override

     .csrf().disable()  // Disable CSRF for simplicity; enable it in production
            .authorizeRequests()
            .anyRequest().authenticated()  // Require authentication for all requests
            .and()
            .addFilterBefore(ipRestrictionFilter, UsernamePasswordAuthenticationFilter.class);  // Add the custom filter before the authentication filter
    }



    planation of SecurityConfig:
@EnableWebSecurity: Enables Spring Security’s web security support and provides the Spring MVC integration.
addFilterBefore(...): Registers the IpRestrictionFilter to run before the UsernamePasswordAuthenticationFilter. This ensures the IP check happens before any authentication logic.
public class IpRestrictionFilter extends OncePerRequestFilter {
