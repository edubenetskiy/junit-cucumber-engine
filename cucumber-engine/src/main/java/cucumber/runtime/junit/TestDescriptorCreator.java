package cucumber.runtime.junit;

import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.model.CucumberExamples;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenario;
import cucumber.runtime.model.CucumberScenarioOutline;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.model.Step;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class TestDescriptorCreator {

    private final UniqueId engineId;
    private final RuntimeOptions runtimeOptions;
    private final Runtime runtime;
    private final MethodResolver methodResolver;

    TestDescriptorCreator(UniqueId uniqueId, RuntimeOptions runtimeOptions, Runtime runtime, MethodResolver methodResolver) {
        this.engineId = uniqueId;
        this.runtimeOptions = runtimeOptions;
        this.runtime = runtime;
        this.methodResolver = methodResolver;
    }

    CucumberEngineDescriptor createEngineDescriptorFor(List<CucumberFeature> cucumberFeatures) {
        CucumberEngineDescriptor cucumber = new CucumberEngineDescriptor(engineId, runtime, runtimeOptions);
        for (CucumberFeature cucumberFeature : cucumberFeatures) {
            cucumber.addChild(createFeatureDescriptorFor(cucumberFeature));
        }
        return cucumber;
    }

    private FeatureDescriptor createFeatureDescriptorFor(CucumberFeature cucumberFeature) {
        CucumberInsight cucumberInsight = new CucumberInsight(cucumberFeature, methodResolver);
        runtime.getGlue().reportStepDefinitions(cucumberInsight);

        UniqueId featureFileId = engineId.append("feature", cucumberFeature.getGherkinFeature().getId());
        Optional<TestSource> testSource = cucumberInsight.sourcesFor(cucumberFeature);
        FeatureDescriptor result = new FeatureDescriptor(featureFileId, cucumberFeature, testSource);
        for (CucumberTagStatement cucumberTagStatement : cucumberFeature.getFeatureElements()) {
            result.addChild(createDescriptorFor(featureFileId, cucumberTagStatement, cucumberFeature, cucumberInsight));
        }

        return result;
    }

    private TestDescriptor createDescriptorFor(UniqueId parentId, CucumberTagStatement cucumberTagStatement, CucumberFeature cucumberFeature, CucumberInsight cucumberInsight) {
        if (cucumberTagStatement instanceof CucumberScenario) {
            return createScenarioDescriptorFor((CucumberScenario) cucumberTagStatement, parentId, cucumberInsight);
        }
        return createOutlineDescriptorFor((CucumberScenarioOutline) cucumberTagStatement, parentId, cucumberFeature);
    }

    private ScenarioDescriptor createScenarioDescriptorFor(CucumberScenario cucumberScenario, UniqueId parentId, CucumberInsight cucumberInsight) {
        final UniqueId scenarioId = parentId.append("scenario", extractId(cucumberScenario));
        Optional<TestSource> scenarioSource = cucumberInsight.sourcesFor(cucumberScenario);
        final ScenarioDescriptor descriptor = new ScenarioDescriptor(scenarioId, cucumberScenario.getVisualName(), cucumberScenario, scenarioSource);

        List<Step> allSteps = new ArrayList<>();
        if (null != cucumberScenario.getCucumberBackground()) {
            for (Step backgroundStep : cucumberScenario.getCucumberBackground().getSteps()) {
                Step copy = new Step(
                        backgroundStep.getComments(),
                        backgroundStep.getKeyword(),
                        backgroundStep.getName(),
                        backgroundStep.getLine(),
                        backgroundStep.getRows(),
                        backgroundStep.getDocString()
                );
                allSteps.add(copy);
            }
        }

        allSteps.addAll(cucumberScenario.getSteps());
        for (Step step : allSteps) {
            Optional<TestSource> stepSource = cucumberInsight.sourcesFor(step);
            StepDescriptor stepDescriptor = new StepDescriptor(scenarioId.append("step", step.getName()), DisplayNames.displayNameFor(step), step, stepSource);
            descriptor.addChild(stepDescriptor);
        }
        return descriptor;
    }

    private OutlineDescriptor createOutlineDescriptorFor(CucumberScenarioOutline cucumberScenarioOutline, UniqueId parentId, CucumberFeature cucumberFeature) {
        UniqueId scenarioOutlineId = parentId.append("scenario-outline", extractId(cucumberScenarioOutline));
        OutlineDescriptor descriptor = new OutlineDescriptor(scenarioOutlineId, cucumberScenarioOutline.getVisualName(), cucumberScenarioOutline);

        for (CucumberExamples cucumberExamples : cucumberScenarioOutline.getCucumberExamplesList()) {
            List<CucumberScenario> exampleScenarios = cucumberExamples.createExampleScenarios();
            for (CucumberScenario exampleScenario : exampleScenarios) {
                descriptor.addChild(createScenarioDescriptorFor(exampleScenario, scenarioOutlineId, new CucumberInsight(cucumberFeature, methodResolver)));
            }
        }
        return descriptor;
    }

    private String extractId(CucumberTagStatement cucumberScenario) {
        return cucumberScenario.getGherkinModel().getId();
    }

}
