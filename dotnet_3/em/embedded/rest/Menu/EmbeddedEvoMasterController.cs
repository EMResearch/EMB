﻿using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using EvoMaster.Controller;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Problem;
using Npgsql;
using EvoMaster.Controller.Controllers.db;
using EvoMaster.DatabaseStarter;
using EvoMaster.DatabaseStarter.Abstractions;

namespace Menu
{
    public class EmbeddedEvoMasterController : EmbeddedSutController
    {
        private bool _isSutRunning;
        private int _sutPort;
        private NpgsqlConnection _connection;
        private IDatabaseStarter _databaseStarter;

        private static void Main(string[] args)
        {
            var embeddedEvoMasterController = new EmbeddedEvoMasterController();

            var instrumentedSutStarter = new InstrumentedSutStarter(embeddedEvoMasterController);

            Console.WriteLine("Driver is starting...\n");

            instrumentedSutStarter.Start();
        }

        public override string GetDatabaseDriverName() => null;

        public override List<AuthenticationDto> GetInfoForAuthentication() => null;

        public override string GetPackagePrefixesToCover() => "Menu.API";

        public override OutputFormat GetPreferredOutputFormat() => OutputFormat.CSHARP_XUNIT;

        //TODO: check again
        public override IProblemInfo GetProblemInfo() =>
            GetSutPort() != 0
                ? new RestProblem("http://localhost:" + GetSutPort() + "/swagger/v1/swagger.json", null)
                : new RestProblem(null, null);

        public override bool IsSutRunning() => _isSutRunning;

        public override void ResetStateOfSut()
        {
            DbCleaner.ClearDatabase_Postgres(_connection);
        }

        public override string StartSut()
        {
            var ephemeralPort = GetEphemeralTcpPort();

            Task.Run(async () =>
            {
                var dbPort = GetEphemeralTcpPort();

                _databaseStarter = new PostgresDatabaseStarter();

                var (connectionString, dbConnection) =
                    await _databaseStarter.StartAsync("restaurant_menu_database", dbPort);

                _connection = dbConnection as NpgsqlConnection;

                API.Program.Main(new[] {$"{ephemeralPort}", connectionString});
            });

            WaitUntilSutIsRunning(ephemeralPort, 190);

            _sutPort = ephemeralPort;

            _isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut()
        {
            API.Program.Shutdown();

            _connection.Close();
            //TODO
            // _database.StopAsync().GetAwaiter().GetResult();

            _isSutRunning = false;
        }

        protected int GetSutPort() => _sutPort;
    }
}