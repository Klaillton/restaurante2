package io.github.klaillton.restaurante;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

/** Regras do ADR 0001, no build. Vazias hoje (só esqueleto); passam a morder com as features. */
class FronteirasDosModulosTest {

  private static final String BASE = "io.github.klaillton.restaurante";
  private static final List<String> MODULOS = List.of("pedidos", "cozinha", "admin");
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE);

  @Test
  void umModuloSoImportaOApiDeOutroModulo() {
    for (String modulo : MODULOS) {
      for (String outro : MODULOS) {
        if (modulo.equals(outro)) {
          continue;
        }
        noClasses()
            .that()
            .resideInAPackage(BASE + "." + modulo + "..")
            .should()
            .dependOnClassesThat(
                resideInAPackage(BASE + "." + outro + "..")
                    .and(not(resideInAPackage(BASE + "." + outro + ".api.."))))
            .allowEmptyShould(true)
            .check(CLASSES);
      }
    }
  }

  @Test
  void partilhadoNaoDependeDeModulos() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".partilhado..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(BASE + ".pedidos..", BASE + ".cozinha..", BASE + ".admin..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void partilhadoNaoTemAnotacoesSpringNemJpa() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".partilhado..")
        .should()
        .beMetaAnnotatedWith(Component.class)
        .orShould()
        .beAnnotatedWith("jakarta.persistence.Entity")
        .orShould()
        .beAnnotatedWith("org.springframework.data.relational.core.mapping.Table")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void naoHaCiclosEntreModulos() {
    slices()
        .matching(BASE + ".(*)..")
        .should()
        .beFreeOfCycles()
        .allowEmptyShould(true)
        .check(CLASSES);
  }
}
