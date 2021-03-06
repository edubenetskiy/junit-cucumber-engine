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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

class CucumberEngineFixture {
    static Runnable whereExecutionFails() {
        return () -> {
            throw new RuntimeException();
        };
    }

    private static Runnable whereExecutionResultDoesNotMatter() {
        return whereExecutionSucceeds();
    }

    static Runnable whereExecutionSucceeds() {
        return () -> {
            //no exception marks success
        };
    }

    private final CucumberEngine engine = new CucumberEngine();
    final CapturingEngineExecutionListener engineExecutionListener = new CapturingEngineExecutionListener();
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final RuntimeGlue glue = new RuntimeGlue(new UndefinedStepsTracker(), new LocalizedXStreams(classLoader));

    void run(CucumberFeatureBuilder feature) {
        CucumberEngineDescriptor cucumberEngineDescriptor = discoveredDescriptorsFor(feature);
        engine.execute(new ExecutionRequest(cucumberEngineDescriptor, engineExecutionListener, new NoConfigurationParameters()));
    }

    CucumberEngineDescriptor discoveredDescriptorsFor(CucumberFeatureBuilder feature) {
        UniqueId engineId = UniqueId.forEngine(CucumberEngine.ENGINE_ID);
        RuntimeOptions runtimeOptions = new RuntimeOptions("");
        Collection<? extends Backend> backends = Collections.singleton(new BackendStub());
        Runtime runtime = new Runtime(null, classLoader, backends, runtimeOptions, StopWatch.SYSTEM, glue);

        MethodResolver methodResolver = alwaysResolveToString();
        return new TestDescriptorCreator(engineId, runtimeOptions, runtime, methodResolver).createEngineDescriptorFor(Collections.singletonList(feature.build()));
    }

    ExecutionRecord executionRecordFor(String stepText) {
        return engineExecutionListener.executionRecordFor(stepText);
    }

    void beforeHookImplementation(Runnable runnable) {
        glue.addBeforeHook(new ConfigurableHookDefinition(runnable));
    }

    void stepImplementationFor(String stepText) {
        stepImplementationFor(stepText, whereExecutionResultDoesNotMatter());
    }

    void stepImplementationFor(String stepText, Runnable stepExecution) {
        glue.addStepDefinition(new ConfigurableStepDefinition(stepText, stepExecution));
    }

    private MethodResolver alwaysResolveToString() {
        return new MethodResolver(){
            @Override
            Method resolve(String signatureWithPath) {
                try {
                    return Object.class.getMethod("toString");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("There is no toString method on object. Nice!");
                }
            }
        };
    }

}
