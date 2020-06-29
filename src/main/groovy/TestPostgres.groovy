import org.postgresql.PGProperty
import org.postgresql.jdbc.GSSEncMode

@groovy.transform.CompileStatic
class TestPostgres {

    public static void main(String[] args) {
        new TestPostgres().testKerberos()
    }

    public void testKerberos() {
        String host='127.0.0.1'
        String superUser = 'davec'
        String superPass = 'test'
        PgJDBC pgJDBC

        Kerberos kerberos = new Kerberos()
        Process k = kerberos.startKerberos()
        Postgres postgres = new Postgres('/usr/lib/postgresql/11/bin/', '/tmp/pggss')
        postgres.writePgHBA("host all all 127.0.0.1/32 trust")
        Process p = postgres.startPostgres(kerberos.env)
        pgJDBC = new PgJDBC(host, postgres.getPort());
        pgJDBC.addProperty(PGProperty.GSS_ENC_MODE, GSSEncMode.DISABLE.value)
        pgJDBC.createUser(superUser, superPass, 'test1', 'secret1')
        pgJDBC.createDatabase(superUser, superPass, 'test1', 'test')
        postgres.enableGSS('127.0.0.1', 'hostgssenc', 'map=mymap')
        postgres.enableMyMap('EXAMPLE.COM')
        postgres.setKeyTabLocation(kerberos.getKeytab())
        postgres.reload()
        pgJDBC.addProperty(PGProperty.GSS_ENC_MODE, GSSEncMode.PREFER.value)
        pgJDBC.addProperty(PGProperty.JAAS_LOGIN, true)
        pgJDBC.addProperty(PGProperty.JAAS_APPLICATION_NAME,"pgjdbc")
        try {
            pgJDBC.tryConnect('test', 'auth-test-localhost.postgresql.example.com', postgres.getPort(), 'test1', 'secret1')
        } finally {
            !p.destroy()
            !kerberos.destroy()
        }
    }
}
