package org.openapitools.codegen.languages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;
import org.openapitools.codegen.languages.features.UseGenericResponseFeatures;

public class SmartDataCodegenTest extends AbstractCodegenTest {

	@DisplayName("model pojo")
	@Test
	void modelPojo() {
		generate(configurator(SPEC_MODEL, "schemaTest"));
	}

	static void generate(CodegenConfigurator configurator) {
		var gen = new DefaultGenerator();
		gen.setGeneratorPropertyDefault(CodegenConstants.MODELS, "true");
		gen.opts(configurator.toClientOptInput()).generate();
	}
}
