package au.com.reece.deliveryengineering.bamboospecs;

import au.com.reece.deliveryengineering.bamboospecs.models.DeploymentModel;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.UserPasswordCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class DeploymentControl {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentControl.class);

    static void run(UserPasswordCredentials adminUser, String filePath, boolean publish) {
        run(adminUser, new File(filePath), publish);
    }

    static void run(UserPasswordCredentials adminUser, File yamlFile, boolean publish) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        DeploymentModel yamlDeployment;
        try {
            yamlDeployment = mapper.readValue(yamlFile, DeploymentModel.class);
            Set<ConstraintViolation<DeploymentModel>> violations = validator.validate(yamlDeployment);
            if (!violations.isEmpty()) {
                violations.forEach(x -> LOGGER.error("{}: {}", x.getPropertyPath(), x.getMessage()));
                return;
            }
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage());
            return;
        } catch (IOException e) {
            throw new RuntimeException("Error reading YAML file", e);
        }

        // set the file path to the yaml file for includes
        yamlDeployment.yamlPath = yamlFile.getParentFile().getAbsolutePath();

        if (publish) {
            BambooServer bambooServer = new BambooServer(yamlDeployment.bambooServer, adminUser);
            yamlDeployment.publish(bambooServer, adminUser);
        } else {
            yamlDeployment.asDeployment();
            LOGGER.info("YAML parsed OK");
        }
    }
}
