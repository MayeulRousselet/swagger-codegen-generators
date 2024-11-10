package io.swagger.codegen.v3.generators.cpp;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.generators.AbstractCodegenTest;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.java.JavaClientCodegen;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.junit.rules.TemporaryFolder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class CppModelTest extends AbstractCodegenTest {

    private TemporaryFolder folder = new TemporaryFolder();

    @Test(description = "convert a simple java model")
    public void simpleModelTest() {
        final Schema model = new Schema()
            .description("a sample model")
            .addProperties("id", new IntegerSchema().format(SchemaTypeUtil.INTEGER64_FORMAT))
            .addProperties("name", new StringSchema()
                .example("Tony"))
            .addProperties("createdAt", new DateTimeSchema())
            .addRequiredItem("id")
            .addRequiredItem("name");
        final DefaultCodegenConfig codegen = new JavaClientCodegen();
        codegen.preprocessOpenAPI(new OpenAPI().components(new Components()));
        final CodegenModel cm = codegen.fromModel("sample", model);

        Assert.assertEquals(cm.name, "sample");
        Assert.assertEquals(cm.classname, "Sample");
        Assert.assertEquals(cm.description, "a sample model");
        Assert.assertEquals(cm.vars.size(), 3);

        final List<CodegenProperty> vars = cm.vars;

        final CodegenProperty property1 = vars.get(0);
        Assert.assertEquals(property1.baseName, "id");
        Assert.assertEquals(property1.getter, "getId");
        Assert.assertEquals(property1.setter, "setId");
        Assert.assertEquals(property1.datatype, "Long");
        Assert.assertEquals(property1.name, "id");
        Assert.assertEquals(property1.defaultValue, "null");
        Assert.assertEquals(property1.baseType, "Long");
        Assert.assertTrue(getBooleanValue(property1, CodegenConstants.HAS_MORE_EXT_NAME));
        Assert.assertTrue(property1.required);
        Assert.assertTrue(getBooleanValue(property1, CodegenConstants.IS_NOT_CONTAINER_EXT_NAME));

        final CodegenProperty property2 = vars.get(1);
        Assert.assertEquals(property2.baseName, "name");
        Assert.assertEquals(property2.getter, "getName");
        Assert.assertEquals(property2.setter, "setName");
        Assert.assertEquals(property2.datatype, "String");
        Assert.assertEquals(property2.name, "name");
        Assert.assertEquals(property2.defaultValue, "null");
        Assert.assertEquals(property2.baseType, "String");
        Assert.assertEquals(property2.example, "Tony");
        Assert.assertTrue(getBooleanValue(property2, CodegenConstants.HAS_MORE_EXT_NAME));
        Assert.assertTrue(property2.required);
        Assert.assertTrue(getBooleanValue(property2, CodegenConstants.IS_NOT_CONTAINER_EXT_NAME));

        final CodegenProperty property3 = vars.get(2);
        Assert.assertEquals(property3.baseName, "createdAt");
        Assert.assertEquals(property3.getter, "getCreatedAt");
        Assert.assertEquals(property3.setter, "setCreatedAt");
        Assert.assertEquals(property3.datatype, "Date");
        Assert.assertEquals(property3.name, "createdAt");
        Assert.assertEquals(property3.defaultValue, "null");
        Assert.assertEquals(property3.baseType, "Date");
        Assert.assertFalse(getBooleanValue(property3, CodegenConstants.HAS_MORE_EXT_NAME));
        Assert.assertFalse(property3.required);
        Assert.assertTrue(getBooleanValue(property3, CodegenConstants.IS_NOT_CONTAINER_EXT_NAME));
    }
}
