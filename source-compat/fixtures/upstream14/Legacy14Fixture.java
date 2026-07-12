package legacy.fixture;

import eu.kanade.tachiyomi.source.CatalogueSource;
import eu.kanade.tachiyomi.source.Source;
import eu.kanade.tachiyomi.source.SourceFactory;
import eu.kanade.tachiyomi.source.model.FilterList;
import eu.kanade.tachiyomi.source.model.MangasPage;
import eu.kanade.tachiyomi.source.model.Page;
import eu.kanade.tachiyomi.source.model.SChapter;
import eu.kanade.tachiyomi.source.model.SManga;
import kotlin.coroutines.Continuation;
import rx.Observable;

import java.util.Collections;
import java.util.List;

public final class Legacy14Fixture implements CatalogueSource {
    @Override public long getId() { return 14L; }
    @Override public String getName() { return "Upstream 1.4 fixture"; }
    @Override public String getLang() { return "en"; }
    @Override public boolean getSupportsLatest() { return true; }
    @Override public FilterList getFilterList() { return new FilterList(); }

    @Override public Observable<MangasPage> fetchPopularManga(int page) {
        return Observable.just(new MangasPage(Collections.singletonList(manga("Legacy manga")), false));
    }

    @Override public Observable<MangasPage> fetchLatestUpdates(int page) { return fetchPopularManga(page); }

    @Override public Observable<MangasPage> fetchSearchManga(int page, String query, FilterList filters) {
        return fetchPopularManga(page);
    }

    @Override public Observable<SManga> fetchMangaDetails(SManga manga) {
        return Observable.just(manga("Legacy manga details"));
    }

    @Override public Observable<List<SChapter>> fetchChapterList(SManga manga) {
        SChapter chapter = SChapter.Companion.create();
        chapter.setUrl("/chapter-1");
        chapter.setName("Chapter 1");
        return Observable.just(Collections.singletonList(chapter));
    }

    @Override public Observable<List<Page>> fetchPageList(SChapter chapter) {
        return Observable.just(Collections.emptyList());
    }

    public Object callLegacyMangaDetails(Continuation<? super SManga> continuation) {
        return Source.DefaultImpls.getMangaDetails(this, manga("Input"), continuation);
    }

    public Object callLegacyChapterList(Continuation<? super List<? extends SChapter>> continuation) {
        return CatalogueSource.DefaultImpls.getChapterList(this, manga("Input"), continuation);
    }

    private static SManga manga(String title) {
        SManga manga = SManga.Companion.create();
        manga.setUrl("/manga");
        manga.setTitle(title);
        manga.setInitialized(true);
        return manga;
    }

    public static final class Factory implements SourceFactory {
        @Override public List<Source> createSources() {
            return Collections.singletonList(new Legacy14Fixture());
        }
    }
}
