package cucumber.runtime.junit;

import cucumber.runtime.Backend;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeGlue;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.StopWatch;
import cucumber.runtime.UndefinedStepsTracker;
import cucumber.runtime.xstream.LocalizedXStreams;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.UniqueId;

import java.util.Collection;
import java.util.Collections;

class CucumberEngineFixture {
    static Runnable stepExecutionFails() {
        return () -> {
            throw new RuntimeException();
        };
    }

    static Runnable stepExecutionSucceeds() {
        return () -> {
            //no exception marks success
        };
    }

    private final CucumberEngine engine = new CucumberEngine();
    final CapturingEngineExecutionListener engineExecutionListener = new CapturingEngineExecutionListener();
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final RuntimeGlue glue = new RuntimeGlue(new UndefinedStepsTracker(), new LocalizedXStreams(classLoader));

    void run(CucumberFeatureBuilder feature) {
        UniqueId engineId = UniqueId.forEngine(CucumberEngine.ENGINE_ID);
        RuntimeOptions runtimeOptions = new RuntimeOptions("");
        Collection<? extends Backend> backends = Collections.singleton(new BackendStub());
        Runtime runtime = new Runtime(null, classLoader, backends, runtimeOptions, StopWatch.SYSTEM, glue);
        CucumberEngineDescriptor cucumberEngineDescriptor = new CucumberEngineDescriptor(engineId, runtime, runtimeOptions);
        cucumberEngineDescriptor.addChild(engine.createDescriptorFor(engineId, feature.build()));
        engine.execute(new ExecutionRequest(cucumberEngineDescriptor, engineExecutionListener, new NoConfigurationParameters()));
    }

    ExecutionRecord executionReportFor(String stepText) {
        return engineExecutionListener.executionRecordFor(stepText);
    }

    void addStepDefinitionFor(String stepText, Runnable stepExecution) {
        glue.addStepDefinition(new ConfigurableStepDefinition(stepText, stepExecution));
    }
}