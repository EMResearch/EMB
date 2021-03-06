﻿using System.Collections.Generic;
using System.Threading.Tasks;
using CaseStudies.Rest.Library;
using EvoMaster.Controller;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Problem;

namespace Embedded.Rest.Library {
    public class EmbeddedEvoMasterController : EmbeddedSutController {

        private bool isSutRunning;
        private int sutPort;

        static void Main (string[] args) {

            var embeddedEvoMasterController = new EmbeddedEvoMasterController ();

            InstrumentedSutStarter instrumentedSutStarter = new InstrumentedSutStarter (embeddedEvoMasterController);

            System.Console.WriteLine ("Driver is starting...\n");

            instrumentedSutStarter.Start ();
        }

        public override string GetDatabaseDriverName () => null;

        public override List<AuthenticationDto> GetInfoForAuthentication () => null;

        public override string GetPackagePrefixesToCover () => "CaseStudies.Rest.Library";

        //TODO: later on we should create sth specific for C#
        public override OutputFormat GetPreferredOutputFormat () => OutputFormat.JAVA_JUNIT_5;

        //TODO: check again
        public override IProblemInfo GetProblemInfo () =>
            GetSutPort () != 0 ? new RestProblem ("http://localhost:" + GetSutPort () + "/swagger/v1/swagger.json", null) : new RestProblem (null, null);

        public override bool IsSutRunning () => isSutRunning;

        public override void ResetStateOfSut () { }


        public override string StartSut () {
            //TODO: check this again
            int ephemeralPort = GetEphemeralTcpPort ();

            var task = Task.Run (() => {

                CaseStudies.Rest.Library.Program.Main (new string[] { ephemeralPort.ToString () });
            });

            WaitUntilSutIsRunning (ephemeralPort);

            sutPort = ephemeralPort;

            isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut () {

            CaseStudies.Rest.Library.Program.Shutdown ();

            isSutRunning = false;
        }

        protected int GetSutPort () => sutPort;
    }
}