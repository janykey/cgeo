package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.content.res.Resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapsforge.v3.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.v3.map.reader.MapDatabase;
import org.mapsforge.v3.map.reader.header.FileOpenResult;

public final class MapsforgeMapProvider extends AbstractMapProvider {

    public static final String MAPSFORGE_MAPNIK_ID = "MAPSFORGE_MAPNIK";
    private MapItemFactory mapItemFactory = new MapsforgeMapItemFactory();

    private MapsforgeMapProvider() {
        final Resources resources = CgeoApplication.getInstance().getResources();

        registerMapSource(new MapsforgeMapSource(MAPSFORGE_MAPNIK_ID, this, resources.getString(R.string.map_source_osm_mapnik), MapGeneratorInternal.MAPNIK));

        updateOfflineMaps();
    }

    private static final class Holder {
        private static final MapsforgeMapProvider INSTANCE = new MapsforgeMapProvider();
    }

    public static MapsforgeMapProvider getInstance() {
        return Holder.INSTANCE;
    }

    public static List<String> getOfflineMaps() {
        final String directoryPath = Settings.getMapFileDirectory();
        if (StringUtils.isBlank(directoryPath)) {
            return Collections.emptyList();
        }

        final File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            try {
                final List<String> mapFileList = new ArrayList<>();
                final File[] files = directory.listFiles();
                if (ArrayUtils.isNotEmpty(files)) {
                    for (final File file : files) {
                        if (file.getName().endsWith(".map") && isValidMapFile(file.getAbsolutePath())) {
                            mapFileList.add(file.getAbsolutePath());
                        }
                    }
                    Collections.sort(mapFileList, TextUtils.COLLATOR);
                }
                return mapFileList;
            } catch (final Exception e) {
                Log.e("MapsforgeMapProvider.getOfflineMaps: ", e);
            }
        }
        return Collections.emptyList();
    }

    public static boolean isValidMapFile(final String mapFileIn) {

        if (StringUtils.isEmpty(mapFileIn)) {
            return false;
        }

        final MapDatabase mapDB = new MapDatabase();
        final FileOpenResult result = mapDB.openFile(new File(mapFileIn));
        mapDB.closeFile();

        return result.isSuccess();
    }

    @Override
    public boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return source1.getNumericalId() == source2.getNumericalId() || (!(source1 instanceof OfflineMapSource) && !(source2 instanceof OfflineMapSource));
    }

    @Override
    public Class<? extends Activity> getMapClass() {
        mapItemFactory = new MapsforgeMapItemFactory();
        return MapsforgeMapActivity.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.mfmap;
    }

    @Override
    public int getMapLayoutId() {
        return R.layout.map_mapsforge;
    }

    @Override
    public MapItemFactory getMapItemFactory() {
        return mapItemFactory;
    }

    /**
     * Offline maps use the hash of the filename as ID. That way changed files can easily be detected. Also we do no
     * longer need to differentiate between internal map sources and offline map sources, as they all just have an
     * numerical ID (based on the hash code).
     */
    public static final class OfflineMapSource extends MapsforgeMapSource {

        private final String fileName;

        public OfflineMapSource(final String fileName, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(fileName, mapProvider, name, generator);
            this.fileName = fileName;
        }

        @Override
        public boolean isAvailable() {
            return isValidMapFile(fileName);
        }

        public String getFileName() {
            return fileName;
        }
    }

    public void updateOfflineMaps() {
        MapProviderFactory.deleteOfflineMapSources();
        final Resources resources = CgeoApplication.getInstance().getResources();
        final List<String> offlineMaps = getOfflineMaps();
        for (final String mapFile : offlineMaps) {
            final String mapName = StringUtils.capitalize(StringUtils.substringBeforeLast(new File(mapFile).getName(), "."));
            registerMapSource(new OfflineMapSource(mapFile, this, mapName + " (" + resources.getString(R.string.map_source_osm_offline) + ")", MapGeneratorInternal.DATABASE_RENDERER));
        }
    }
}
