package com.cabex.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sert le frontend React (SPA) copié dans {@code src/main/resources/static} :
 * la racine « / » sert {@code index.html}, les routes inconnues (ex. « /services »)
 * réinitialisent sur {@code index.html} pour que le routeur React prenne le relais.
 *
 * <p>Les chemins {@code /api/**} et autres points techniques, ainsi que les URI
 * d'asset statique introuvables (ex. /assets/absent.js), ne sont PAS renvoyés
 * vers l'index — Spring répond 404 comme attendu.</p>
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    private static final Pattern EXTENSION_STATIQUE =
            Pattern.compile(".*\\.(js|css|png|jpe?g|jpeg|svg|gif|ico|webp|map|json|txt|woff2?|ttf|otf|eot)$",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new ResourceResolver() {

                    @Override
                    public Resource resolveResource(jakarta.servlet.http.HttpServletRequest request,
                                                    String requestPath,
                                                    List<? extends Resource> locations,
                                                    ResourceResolverChain chain) {
                        Resource resolved = chain.resolveResource(request, requestPath, locations);
                        if (resolved != null && resolved.isReadable()) {
                            return resolved;
                        }
                        if (!renvoyerVersIndex(request.getRequestURI())) {
                            return null;
                        }
                        return chain.resolveResource(request, "index.html", locations);
                    }

                    @Override
                    public String resolveUrlPath(String resourcePath,
                                                 List<? extends Resource> locations,
                                                 ResourceResolverChain chain) {
                        String resolved = chain.resolveUrlPath(resourcePath, locations);
                        if (resolved != null) {
                            return resolved;
                        }
                        if (!renvoyerVersIndex("/" + resourcePath)) {
                            return null;
                        }
                        return chain.resolveUrlPath("index.html", locations);
                    }

                    /** @return true si le chemin doit servir l'index.html du SPA. */
                    private boolean renvoyerVersIndex(String uriPath) {
                        if (uriPath == null) {
                            return false;
                        }
                        String path = uriPath;
                        if (path.startsWith("/api/") || path.startsWith("/v3/")
                                || path.startsWith("/swagger-ui") || path.startsWith("/actuator")) {
                            return false;
                        }
                        // Pas de renvoi vers l'index pour une URI qui ressemble à un fichier statique.
                        return !EXTENSION_STATIQUE.matcher(path).matches();
                    }
                });
    }
}
