package io.github.mike10004.debianmaven;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

import static java.util.Objects.requireNonNull;

public class LogBucket extends DefaultLog {

    private final CollectingLogger collector;

    public LogBucket() {
        this(new CollectingLogger());
    }

    private LogBucket(CollectingLogger collector) {
        super(collector);
        this.collector = requireNonNull(collector);
    }

    public String dump() {
        return collector.dump();
    }

    /**
     * Reimplementation of {@link org.codehaus.plexus.logging.console.ConsoleLogger}
     * that captures everything in a buffer.
     */
    private static class CollectingLogger extends org.codehaus.plexus.logging.AbstractLogger {

        private final StringWriter bucket;
        private final PrintWriter out;

        // ----------------------------------------------------------------------
        // Constants
        // ----------------------------------------------------------------------

        private static final String[] TAGS = { "[DEBUG] ", "[INFO] ", "[WARNING] ", "[ERROR] ", "[FATAL ERROR] " };

        // ----------------------------------------------------------------------
        // Constructors
        // ----------------------------------------------------------------------

        public CollectingLogger( final int threshold, final String name )
        {
            super( threshold, name );
            bucket = new StringWriter(1024);
            out = new PrintWriter(bucket, true);
        }

        public CollectingLogger()
        {
            this( Logger.LEVEL_INFO, "console" );
        }

        public String dump() {
            bucket.flush();
            return bucket.toString();
        }

        // ----------------------------------------------------------------------
        // Public methods
        // ----------------------------------------------------------------------

        public void debug( final String message, final Throwable throwable )
        {
            if ( isDebugEnabled() )
            {
                log( LEVEL_DEBUG, message, throwable );
            }
        }

        public void info( final String message, final Throwable throwable )
        {
            if ( isInfoEnabled() )
            {
                log( LEVEL_INFO, message, throwable );
            }
        }

        public void warn( final String message, final Throwable throwable )
        {
            if ( isWarnEnabled() )
            {
                log( LEVEL_WARN, message, throwable );
            }
        }

        public void error( final String message, final Throwable throwable )
        {
            if ( isErrorEnabled() )
            {
                log( LEVEL_ERROR, message, throwable );
            }
        }

        public void fatalError( final String message, final Throwable throwable )
        {
            if ( isFatalErrorEnabled() )
            {
                log( LEVEL_FATAL, message, throwable );
            }
        }

        public Logger getChildLogger( final String name )
        {
            return this;
        }

        // ----------------------------------------------------------------------
        // Implementation methods
        // ----------------------------------------------------------------------

        private void log( final int level, final String message, final Throwable throwable )
        {
            out.println( TAGS[level].concat( message ) );
            if ( throwable != null )
            {
                throwable.printStackTrace( out );
            }
        }
    }
}
