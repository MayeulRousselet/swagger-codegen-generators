package io.swagger.codegen.v3.generators.cpp;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class CrowGeneratorCodegenTest {

    @Test(description = "verify that parameters are listed in following order: header, query, path, cookie, body (OAS 3.x)")
    public void testParameterOrdersUseOas3() throws Exception {
        final TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        final File output = folder.getRoot();

        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setLang("crow")
            .setInputSpecURL("src/test/resources/3_0_0/parameterOrder.yaml")
            .setOutputDir(output.getAbsolutePath());

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        new DefaultGenerator().opts(clientOptInput).generate();

        final File adminApiFile = new File(output, "include/admin_api.h");
        final String content = FileUtils.readFileToString(adminApiFile);

        Assert.assertTrue(content.contains("ResponseEntity<LocalizedText> updateTest(int64_t id)"));
        Assert.assertTrue(content.contains("const LocalizedText& body"));

        //final File adminApiControllerFile = new File(output, "/src/main/java/io/swagger/api/AdminApiController.java");
        //final String contentAdminApiController = FileUtils.readFileToString(adminApiControllerFile);

        //Assert.assertFalse(contentAdminApiController.contains("jakarta"));
        //Assert.assertTrue(contentAdminApiController.contains("javax"));

        folder.delete();
    }

}
