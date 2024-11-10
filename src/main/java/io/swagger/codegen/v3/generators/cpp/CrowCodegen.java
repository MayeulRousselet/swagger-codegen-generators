package io.swagger.codegen.v3.generators.cpp;


import com.fasterxml.jackson.databind.ser.std.MapProperty;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.models.Response;
import io.swagger.models.apideclaration.Model;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrowCodegen extends AbstractCppCodegen {
    protected String implFolder = "impl";

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "crow";
    }

    @Override
    public String getHelp() {
        return "Generates a C++ API server (based on Pistache)";
    }

    public CrowCodegen() {
        super();

        apiPackage = "io.swagger.api";
        modelPackage = "io.swagger.model";

        modelTemplateFiles.put("model-header.mustache", ".h");
        modelTemplateFiles.put("model-source.mustache", ".cpp");

        apiTemplateFiles.put("api-header.mustache", ".h");
        apiTemplateFiles.put("api-source.mustache", ".cpp");
        apiTemplateFiles.put("api-impl-header.mustache", ".h");
        apiTemplateFiles.put("api-impl-source.mustache", ".cpp");
        apiTemplateFiles.put("main-api-server.mustache", ".cpp");

        embeddedTemplateDir = templateDir = "crow";

        cliOptions.clear();

        reservedWords = new HashSet<>();

        supportingFiles.add(new SupportingFile("modelbase-header.mustache", "model", "ModelBase.h"));
        supportingFiles.add(new SupportingFile("modelbase-source.mustache", "model", "ModelBase.cpp"));
        supportingFiles.add(new SupportingFile("cmake.mustache", "", "CMakeLists.txt"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList("int", "char", "bool", "long", "float", "double", "int32_t", "int64_t"));

        typeMapping = new HashMap<String, String>();
        typeMapping.put("date", "std::string");
        typeMapping.put("DateTime", "std::string");
        typeMapping.put("string", "std::string");
        typeMapping.put("integer", "int32_t");
        typeMapping.put("long", "int64_t");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "std::vector");
        typeMapping.put("map", "std::map");
        typeMapping.put("file", "std::string");
        typeMapping.put("object", "Object");
        typeMapping.put("binary", "std::string");
        typeMapping.put("number", "double");
        typeMapping.put("UUID", "std::string");

        super.importMapping = new HashMap<String, String>();
        importMapping.put("std::vector", "#include <vector>");
        importMapping.put("std::map", "#include <map>");
        importMapping.put("std::string", "#include <string>");
        importMapping.put("Object", "#include \"Object.h\"");
    }

    @Override
    public void processOpts() {
        super.processOpts();

        additionalProperties.put("modelNamespaceDeclarations", modelPackage.split("\\."));
        additionalProperties.put("modelNamespace", modelPackage.replaceAll("\\.", "::"));
        additionalProperties.put("apiNamespaceDeclarations", apiPackage.split("\\."));
        additionalProperties.put("apiNamespace", apiPackage.replaceAll("\\.", "::"));
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle
     * escaping those terms here. This logic is only called if a variable
     * matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "_" + name; // add an underscore to the name
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        } else {
            return "#include \"" + name + ".h\"";
        }
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, schema, allDefinitions);

        Set<String> oldImports = codegenModel.imports;
        codegenModel.imports = new HashSet<>();
        for (String imp : oldImports) {
            String newImp = toModelImport(imp);
            if (!newImp.isEmpty()) {
                codegenModel.imports.add(newImp);
            }
        }

        return codegenModel;
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation,
                                          Map<String, Schema> schemas, OpenAPI openAPI) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, schemas, openAPI);

        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            ApiResponse methodResponse = findMethodResponse(operation.getResponses());
            if (methodResponse != null) {
        /*        if (methodResponse.getSchema() != null) {
                    CodegenProperty cm = fromProperty("response", methodResponse.getSchema());
                    op.vendorExtensions.put("x-codegen-response", cm);
                    if(cm.datatype == "HttpContent") {
                        op.vendorExtensions.put("x-codegen-response-ishttpcontent", true);
                    }
                }*/
            }
        }

        String pathForPistache = path.replaceAll("\\{(.*?)}", ":$1");
        op.vendorExtensions.put("x-codegen-pistache-path", pathForPistache);

        return op;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        String classname = (String) operations.get("classname");
        operations.put("classnameSnakeUpperCase", DefaultCodegenConfig.underscore(classname).toUpperCase());
        operations.put("classnameSnakeLowerCase", DefaultCodegenConfig.underscore(classname).toLowerCase());

        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation op : operationList) {
            boolean consumeJson = false;
            boolean isParsingSupported = true;
            if (op.bodyParam != null) {
                if (op.bodyParam.vendorExtensions == null) {
                    op.bodyParam.vendorExtensions = new HashMap<>();
                }
                //TODO: support getIsDate() ?
                op.bodyParam.vendorExtensions.put("x-codegen-pistache-isStringOrDate", op.bodyParam.getIsString() || op.bodyParam.getIsDateTime());
            }
            if(op.consumes != null) {
                for (Map<String, String> consume : op.consumes) {
                    if (consume.get("mediaType") != null && consume.get("mediaType").equals("application/json")) {
                        consumeJson = true;
                    }
                }
            }

            op.httpMethod = op.httpMethod.substring(0, 1).toUpperCase() + op.httpMethod.substring(1).toLowerCase();

            for(CodegenParameter param : op.allParams){
                if (param.getIsFormParam()) isParsingSupported=false;
                if (param.getIsFile()) isParsingSupported=false;
                if (param.getIsCookieParam()) isParsingSupported=false;

                //TODO: This changes the info about the real type but it is needed to parse the header params
                if (param.getIsHeaderParam()) {
                    param.dataType = "Optional<Net::Http::Header::Raw>";
                    param.baseType = "Optional<Net::Http::Header::Raw>";
                } else if(param.getIsQueryParam()){
                    if(param.getIsPrimitiveType()) {
                        param.dataType = "Optional<" + param.dataType + ">";
                    } else {
                        param.dataType = "Optional<" + param.baseType + ">";
                        param.baseType = "Optional<" + param.baseType + ">";
                    }
                }
            }

            if (op.vendorExtensions == null) {
                op.vendorExtensions = new HashMap<>();
            }
            op.vendorExtensions.put("x-codegen-pistache-consumesJson", consumeJson);
            op.vendorExtensions.put("x-codegen-pistache-isParsingSupported", isParsingSupported);
        }

        return objs;
    }

    @Override
    public String toModelFilename(String name) {
        return initialCaps(name);
    }

    @Override
    public String apiFilename(String templateName, String tag) {
        String result = super.apiFilename(templateName, tag);

        if ( templateName.endsWith("impl-header.mustache") ) {
            int ix = result.lastIndexOf('/') == -1 ? result.lastIndexOf('\\') : result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 2) + "Impl.h";
            result = result.replace(apiFileFolder(), implFileFolder());
        } else if ( templateName.endsWith("impl-source.mustache") ) {
            int ix = result.lastIndexOf('/') == -1 ? result.lastIndexOf('\\') : result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 4) + "Impl.cpp";
            result = result.replace(apiFileFolder(), implFileFolder());
        } else if ( templateName.endsWith("api-server.mustache") ) {
            int ix = result.lastIndexOf('/') == -1 ? result.lastIndexOf('\\') : result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 4) + "MainServer.cpp";
            result = result.replace(apiFileFolder(), outputFolder);
        }
        return result;
    }

    @Override
    public String getDefaultTemplateDir() {
        return templateDir;
    }

    @Override
    public String toApiFilename(String name) {
        return initialCaps(name) + "Api";
    }

    /**
     * Optional - type declaration. This is a String which is used by the
     * templates to instantiate your types. There is typically special handling
     * for different property types
     *
     * @return a string value used as the `dataType` field for model templates,
     *         `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Schema p) {
        String swaggerType = getSchemaType(p);

        if (p instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return swaggerType + "<" + getTypeDeclaration(inner) + ">";
        }
        if (p instanceof MapSchema) {
            MapSchema mp = (MapSchema) p;
            Object inner = mp.getAdditionalProperties();
            if(inner instanceof ObjectSchema) {
                return swaggerType + "<std::string, " + getTypeDeclaration((ObjectSchema)inner) + ">";
            }
        }
        if (p instanceof StringSchema || p instanceof DateSchema
            || p instanceof DateTimeSchema || p instanceof FileSchema
            || languageSpecificPrimitives.contains(swaggerType)) {
            return toModelName(swaggerType);
        }

        return "std::shared_ptr<" + swaggerType + ">";
    }

    @Override
    public String toDefaultValue(Schema p) {
        if (p instanceof StringSchema) {
            return "\"\"";
        } else if (p instanceof BooleanSchema) {
            return "false";
        } else if (p instanceof DateSchema) {
            return "\"\"";
        } else if (p instanceof DateTimeSchema) {
            return "\"\"";
        } else if (p instanceof NumberSchema) {
            return "0.0";
        }
        /*else if (p instanceof IntegerSchema) {
            return "0L";
        }*/
        else if (p instanceof IntegerSchema) {
            return "0";
        } else if (p instanceof MapSchema) {
            MapSchema ap = (MapSchema) p;
            Object inner = ap.getAdditionalProperties();
            String type;
            if(inner instanceof ObjectSchema) {
                type = getTypeDeclaration((ObjectSchema)inner);
            } else {
                type="Object";
            }
            return "std::map<std::string, " + inner + ">()";
        } else if (p instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) p;
            String inner = getTypeDeclaration(ap.getItems());
            if (!languageSpecificPrimitives.contains(inner)) {
                inner = "std::shared_ptr<" + inner + ">";
            }
            return "std::vector<" + inner + ">()";
        }
        //TODO: handle ref
        /*else if (p instanceof Referez) {
            RefProperty rp = (RefProperty) p;
            return "new " + toModelName(rp.getSimpleRef()) + "()";
        }*/
        return "nullptr";
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);

        boolean isPrimitiveType = parameter.getIsPrimitiveType() == Boolean.TRUE;
        boolean isListContainer = parameter.getIsListContainer() == Boolean.TRUE;
        boolean isString = parameter.getIsString() == Boolean.TRUE;

        if (!isPrimitiveType && !isListContainer && !isString && !parameter.dataType.startsWith("std::shared_ptr")) {
            parameter.dataType = "std::shared_ptr<" + parameter.dataType + ">";
        }
    }

    /**
     * Location to write model files. You can use the modelPackage() as defined
     * when the class is instantiated
     */
    public String modelFileFolder() {
        return (outputFolder + "/model").replace("/", File.separator);
    }

    /**
     * Location to write api files. You can use the apiPackage() as defined when
     * the class is instantiated
     */
    @Override
    public String apiFileFolder() {
        return (outputFolder + "/api").replace("/", File.separator);
    }

    private String implFileFolder() {
        return (outputFolder + "/" + implFolder).replace("/", File.separator);
    }

    /**
     * Optional - swagger type conversion. This is used to map swagger types in
     * a `Property` into either language specific types via `typeMapping` or
     * into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     */

    @Override
    public String getSchemaType(Schema p) {
        String swaggerType = super.getSchemaType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = swaggerType;
        return toModelName(type);
    }

    @Override
    public String toModelName(String type) {
        if (typeMapping.keySet().contains(type) || typeMapping.values().contains(type)
                || importMapping.values().contains(type) || defaultIncludes.contains(type)
                || languageSpecificPrimitives.contains(type)) {
            return type;
        } else {
            return Character.toUpperCase(type.charAt(0)) + type.substring(1);
        }
    }

    @Override
    public String toApiName(String type) {
        return Character.toUpperCase(type.charAt(0)) + type.substring(1) + "Api";
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }
}
