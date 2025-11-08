package hcen.central.inus.testsupport;

import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;

/**
 * Pequeña utilidad para centralizar la resolución de dependencias Maven
 * para los despliegues de Arquillian, respetando el repositorio local que
 * se utiliza en los builds offline del proyecto.
 */
public final class ArquillianMavenResolver {

    private ArquillianMavenResolver() {
    }

    public static File[] resolve(String... coordinates) {
        ConfigurableMavenResolverSystem resolver = Maven.configureResolver();
        ConfigurableMavenResolverSystem configuredResolver = configureOffline(resolver);
        return configuredResolver
                .loadPomFromFile("pom.xml")
                .resolve(coordinates)
                .withTransitivity()
                .asFile();
    }

    private static ConfigurableMavenResolverSystem configureOffline(ConfigurableMavenResolverSystem resolver) {
        String repoPath = System.getProperty("maven.repo.local");
        if (repoPath == null || repoPath.isBlank()) {
            File defaultRepo = new File("tmp_m2");
            if (defaultRepo.isDirectory()) {
                repoPath = defaultRepo.getAbsolutePath();
                System.setProperty("maven.repo.local", repoPath);
            }
        } else {
            File repoDir = new File(repoPath);
            repoPath = repoDir.getAbsolutePath();
            System.setProperty("maven.repo.local", repoPath);
        }

        if (repoPath != null && !repoPath.isBlank()) {
            return resolver.workOffline().withMavenCentralRepo(false);
        }

        return resolver;
    }
}

