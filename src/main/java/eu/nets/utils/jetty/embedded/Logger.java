package eu.nets.utils.jetty.embedded;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author Kristian Rosenvold
 */
public class Logger
{
    private static Method getLogger = null;

    static{
        try
        {
            Class<?> loggerFactory = Class.forName( "org.slf4j.LoggerFactory" );
            getLogger = loggerFactory.getMethod( "getLogger", Class.class );
        }
        catch ( ClassNotFoundException ignore )
        {
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
    }

    public static void info(Class client, String msg){
        if ( hasSlf4J() )
            infoWithSlf4J( client, msg );
        else
            getLogger(client).info( msg );
    }

    public static void warn(Class client, String msg){
        if ( hasSlf4J() )
            warnWithSlf4J( client, msg );
        else
            getLogger(client).warn( msg );
    }

    private static boolean hasSlf4J()
    {
        return getLogger != null;
    }

    public static void info(Class client, String msg, Object... args){

        if ( hasSlf4J() )
            infoWithSlf4J( client, msg, args );
        else
            getLogger(client).info( msg, args );
    }

    public static void infoWithSlf4J(Class client, String msg){
        LoggerFactory.getLogger( client ).info(  msg );
    }

    public static void warnWithSlf4J(Class client, String msg){
        LoggerFactory.getLogger( client ).warn(  msg );
    }
    public static void infoWithSlf4J(Class client, String msg, Object... args){
        LoggerFactory.getLogger( client ).info(  msg, args );
    }

    private static org.eclipse.jetty.util.log.JavaUtilLog getLogger(Class client) {
        return new org.eclipse.jetty.util.log.JavaUtilLog(client.getName());
    }

}
