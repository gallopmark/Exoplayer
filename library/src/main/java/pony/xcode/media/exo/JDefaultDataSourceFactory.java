package pony.xcode.media.exo;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class JDefaultDataSourceFactory implements DataSource.Factory {

    private final DataSource.Factory baseDataSourceFactory;

    /**
     * Instantiates a new J default data source factory.
     *
     * @param context A context.                for {@link DefaultDataSource}.
     */
    public JDefaultDataSourceFactory(Context context) {
        String userAgent = Util.getUserAgent(context, context.getPackageName());
        this.baseDataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
    }

    @Override
    public DataSource createDataSource() {
        return baseDataSourceFactory.createDataSource();
    }
}
