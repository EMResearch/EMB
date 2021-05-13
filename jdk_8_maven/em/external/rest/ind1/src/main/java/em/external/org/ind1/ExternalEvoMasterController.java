package em.external.org.ind1;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    /*
        config the jar and package here
     */
    private static final String SUT_LOCATION_IND1 = System.getenv("SUT_LOCATION_IND1");
    private static final String SUT_PACKAGE_IND1 = System.getenv("SUT_PACKAGE_IND1");

    public static void main(String[] args) {

        int controllerPort = 40100;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = 12345;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = SUT_LOCATION_IND1;
        if (args.length > 2) {
            jarLocation = args[2];
        }

        int timeoutSeconds = 120;
        if(args.length > 3){
            timeoutSeconds = Integer.parseInt(args[3]);
        }

        String packagesToInstrument = SUT_PACKAGE_IND1;
        if(args.length > 5){
            packagesToInstrument = args[5];
        }

        String command = "java";
        if(args.length > 4){
            command = args[4];
        }


        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation,
                        sutPort, timeoutSeconds, command, packagesToInstrument);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private final int timeoutSeconds;
    private final int sutPort;
    private final String jarLocation;
    private final String packagesToInstrument;

    public ExternalEvoMasterController(){
        this(40100, SUT_LOCATION_IND1, 12345, 120, "java", SUT_PACKAGE_IND1);
    }

    public ExternalEvoMasterController(
            int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command,
            String packagesToInstrument) {

        if(jarLocation==null || jarLocation.isEmpty()){
            throw new IllegalArgumentException("Missing jar location");
        }
        if(packagesToInstrument==null || packagesToInstrument.isEmpty()){
            throw new IllegalArgumentException("Missing packages to instrument");
        }

        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        this.packagesToInstrument = packagesToInstrument;
        setControllerPort(controllerPort);
        setJavaCommand(command);
    }


    @Override
    public String[] getInputParameters() {
        return new String[]{"--server.port=" + sutPort};
    }

    public String[] getJVMParameters() {
        return new String[0];
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:" + sutPort;
    }

    @Override
    public String getPathToExecutableJar() {
        return jarLocation;
    }

    @Override
    public String getLogMessageOfInitializedServer() {
        return "Start Application in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
    }

    @Override
    public void postStart() {

    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
    }


    @Override
    public String getPackagePrefixesToCover() {
        return packagesToInstrument;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        List<String> skipEndpoints = new ArrayList<>();

        return new RestProblem(
                getBaseURL() + "/v3/api-docs",
                skipEndpoints
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public String getDatabaseDriverName() {
        return null;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }
}
