package com.movietrivia.filmfacts.di

import android.content.Context
import com.movietrivia.filmfacts.api.*
import com.movietrivia.filmfacts.domain.*
import com.movietrivia.filmfacts.model.*
import com.movietrivia.filmfacts.viewmodel.UiPromptController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class FilmFactsInjectionModel {

    @Singleton
    @Provides
    fun provideRetrofitClient(): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor {
                it.proceed(applyPadding(it))
            }.build())
            .addConverterFactory(MoshiConverterFactory.create(
                Moshi.Builder()
                    .add(NullableStringAdapter())
                    .addLast(KotlinJsonAdapterFactory())
                    .build()
            ))
            .build()

    @Singleton
    @Provides
    fun provideDiscoverService(retrofit: Retrofit): DiscoverService = retrofit.create(DiscoverService::class.java)

    @Singleton
    @Provides
    fun provideConfigurationService(retrofit: Retrofit): ConfigurationService = retrofit.create(ConfigurationService::class.java)

    @Singleton
    @Provides
    fun providePersonService(retrofit: Retrofit): PersonService = retrofit.create(PersonService::class.java)

    @Singleton
    @Provides
    fun provideMovieService(retrofit: Retrofit): MovieService = retrofit.create(MovieService::class.java)

    @Singleton
    @Provides
    fun provideTvShowService(retrofit: Retrofit): TvShowService = retrofit.create(TvShowService::class.java)

    @Singleton
    @Provides
    fun provideAuthenticationService(retrofit: Retrofit): AuthenticationService = retrofit.create(AuthenticationService::class.java)

    @Singleton
    @Provides
    fun provideAccountService(retrofit: Retrofit): AccountService = retrofit.create(AccountService::class.java)

    @Singleton
    @Provides
    fun provideTmdbDataSource(
        discoverService: DiscoverService,
        configurationService: ConfigurationService,
        personService: PersonService,
        movieService: MovieService,
        tvShowService: TvShowService,
        tooManyRequestsDataSource: TooManyRequestsDataSource
    ) = TmdbDataSource(discoverService, configurationService, personService, movieService, tvShowService, tooManyRequestsDataSource)

    @Singleton
    @Provides
    fun provideAuthenticationDataSource(
        @ApplicationContext context: Context
    ) = CustomTabsDataSource(context)

    @Singleton
    @Provides
    fun provideCustomTabsDataSource(
        authenticationService: AuthenticationService,
        customTabsDataSource: CustomTabsDataSource,
        tooManyRequestsDataSource: TooManyRequestsDataSource
    ) = AuthenticationDataSource(authenticationService, customTabsDataSource, tooManyRequestsDataSource)

    @Singleton
    @Provides
    fun provideSessionDataSource(
        @ApplicationContext context: Context
    ) = SessionDataSource(context)

    @Singleton
    @Provides
    @Named(MOVIE_SETTINGS_KEY)
    fun provideMovieUserSettingsDataSource(
        @ApplicationContext context: Context
    ): UserSettingsDataSource = UserSettingsDataSource(context, MOVIE_SETTINGS_KEY)

    @Singleton
    @Provides
    @Named(TV_SHOW_SETTINGS_KEY)
    fun provideTVShowUserSettingsDataSource(
        @ApplicationContext context: Context
    ): UserSettingsDataSource = UserSettingsDataSource(context, TV_SHOW_SETTINGS_KEY)

    @Singleton
    @Provides
    fun provideAccountDataSource(
        accountService: AccountService,
        tooManyRequestsDataSource: TooManyRequestsDataSource
    ): AccountDataSource = AccountDataSource(accountService, tooManyRequestsDataSource)

    @Singleton
    @Provides
    @Named(RECENT_ACTORS)
    fun provideRecentActorsDataSource(
        @ApplicationContext context: Context
    ): RecentElementsDataSource = RecentElementsDataSource(context, RECENT_ACTORS)

    @Singleton
    @Provides
    @Named(RECENT_MOVIES)
    fun provideRecentMoviesDataSource(
        @ApplicationContext context: Context
    ): RecentElementsDataSource = RecentElementsDataSource(context, RECENT_MOVIES)

    @Singleton
    @Provides
    @Named(RECENT_TV_SHOWS)
    fun provideRecentTvShowsDataSource(
        @ApplicationContext context: Context
    ): RecentElementsDataSource = RecentElementsDataSource(context, RECENT_TV_SHOWS, 20)

    @Singleton
    @Provides
    fun provideRecentFlopsDataSource(): RecentFlopsDataSource = RecentFlopsDataSource()

    @Singleton
    @Provides
    @Named(MOVIE_GENRE_IMAGES)
    fun provideMovieGenreImageDataSource(
        @ApplicationContext context: Context
    ): GenreImageDataSource = GenreImageDataSource(
        context,
        MOVIE_GENRE_IMAGES,
        listOf(
            UiGenre(
                "https://www.themoviedb.org/t/p/original/crLfhuTYadl39DucKK8HcEJctIf.jpg",
                -1
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/ncEsesgOJDNrTUED89hYbA117wo.jpg",
                MovieGenre.ACTION.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/wXsQvli6tWqja51pYxXNG1LFIGV.jpg",
                MovieGenre.ANIMATION.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/r0p9IeVzVP4OULWWw0UsDSCWtKb.jpg",
                MovieGenre.FAMILY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/9xOmYwIKLX8pTlDaLKdrvkao8Ju.jpg",
                MovieGenre.FANTASY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/3N9UcnCJRyLE2B4ZTbxIpWqL4aQ.jpg",
                MovieGenre.HORROR.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/qJeU7KM4nT2C1WpOrwPcSDGFUWE.jpg",
                MovieGenre.ROMANCE.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
                MovieGenre.SCI_FI.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/x4biAVdPVCghBlsVIzB6NmbghIz.jpg",
                MovieGenre.WESTERN.key
            )
        )
    )

    @Singleton
    @Provides
    @Named(TV_GENRE_IMAGES)
    fun provideTvGenreImageDataSource(
        @ApplicationContext context: Context
    ): GenreImageDataSource = GenreImageDataSource(
        context,
        TV_GENRE_IMAGES,
        listOf(
            UiGenre(
                "https://www.themoviedb.org/t/p/original/8jZKtF5kwYHYHtD7FWHM9AwQnCD.jpg",
                -1
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/jqot3Wk1mgq1zB1JhTe4yqL7N0l.jpg",
                TvGenre.ACTION_AND_ADVENTURE.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/kU98MbVVgi72wzceyrEbClZmMFe.jpg",
                TvGenre.ANIMATION.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/bY2J2Jq8rSrKm5xCFtzYzqFh44o.jpg",
                TvGenre.COMEDY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/84XPpjGvxNyExjSuLQe0SzioErt.jpg",
                TvGenre.CRIME.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/yhwHMx4M8ILlCKkVUANTLGfQsHG.jpg",
                TvGenre.FAMILY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/oBUwRT7TbhTeFFTguY1s8RmmJpW.jpg",
                TvGenre.REALITY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/wXSnajAZ5ppTKa8Z5zzWGOK85YH.jpg",
                TvGenre.SCI_FI_AND_FANTASY.key
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/29JYJokZaZJg7SWXnw7FsnTntew.jpg",
                TvGenre.WAR_AND_POLITICS.key,
            ),
            UiGenre(
                "https://www.themoviedb.org/t/p/original/ushVB10gSzyzYoq1Br9hpOBgBGm.jpg",
                TvGenre.WESTERN.key
            )
        )
    )

    @Singleton
    @Provides
    fun provideUnlockedAchievementsDataSource(
        @ApplicationContext context: Context
    ): UnlockedAchievementsDataSource = UnlockedAchievementsDataSource(context)

    @Singleton
    @Provides
    fun provideUserHistoryDataSource(
        @ApplicationContext context: Context,
        calendarProvider: CalendarProvider
    ): UserHistoryDataSource = UserHistoryDataSource(context, calendarProvider)

    @Singleton
    @Provides
    fun provideCalendarProvider(
    ): CalendarProvider = CalendarProvider()

    @Singleton
    @Provides
    fun provideTooManyRequestsDataSource(
        calendarProvider: CalendarProvider
    ): TooManyRequestsDataSource = TooManyRequestsDataSource(calendarProvider)

    @Singleton
    @Provides
    fun provideFilmFactsRepository(
        tmdbDataSource: TmdbDataSource,
        userDataRepository: UserDataRepository
    ) = FilmFactsRepository(tmdbDataSource, userDataRepository)

    @Singleton
    @Provides
    fun provideRecentPromptsRepository(
        @Named(RECENT_ACTORS) recentActorsDataSource: RecentElementsDataSource,
        recentFlopsDataSource: RecentFlopsDataSource,
        @Named(RECENT_MOVIES) recentMoviesDataSource: RecentElementsDataSource,
        @Named(RECENT_MOVIES) recentTvShowsDataSource: RecentElementsDataSource
    ) = RecentPromptsRepository(recentActorsDataSource, recentFlopsDataSource, recentMoviesDataSource, recentTvShowsDataSource)

    @Singleton
    @Provides
    fun provideAuthenticationRepository(
        authenticationDataSource: AuthenticationDataSource
    ) = AuthenticationRepository(
        authenticationDataSource
    )

    @Singleton
    @Provides
    fun provideUserDataRepository(
        @ApplicationContext context: Context,
        authenticationRepository: AuthenticationRepository,
        @Named(MOVIE_SETTINGS_KEY) movieUserSettingsDataSource: UserSettingsDataSource,
        @Named(TV_SHOW_SETTINGS_KEY) tvShowUserSettingsDataSource: UserSettingsDataSource,
        sessionDataSource: SessionDataSource,
        accountDataSource: AccountDataSource
    ) = UserDataRepository(
        context,
        authenticationRepository,
        movieUserSettingsDataSource,
        tvShowUserSettingsDataSource,
        sessionDataSource,
        accountDataSource
    )

    @Singleton
    @Provides
    fun provideUserProgressRepository(
        unlockedAchievementsDataSource: UnlockedAchievementsDataSource,
        userHistoryDataSource: UserHistoryDataSource
    ): UserProgressRepository = UserProgressRepository(unlockedAchievementsDataSource, userHistoryDataSource)

    @Singleton
    @Provides
    @Named(MOVIE_GENRE_IMAGES)
    fun provideMovieGenreImageRepository(
        @Named(MOVIE_GENRE_IMAGES) genreImageDataSource: GenreImageDataSource
    ) = GenreImageRepository(genreImageDataSource)

    @Singleton
    @Provides
    @Named(TV_GENRE_IMAGES)
    fun provideTvGenreImageRepository(
        @Named(TV_GENRE_IMAGES) genreImageDataSource: GenreImageDataSource
    ) = GenreImageRepository(genreImageDataSource)

    @Provides
    fun provideTopGrossingMoviesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetTopGrossingMoviesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

//    @Provides
//    fun provideFloppingMoviesUseCase(
//        @ApplicationContext context: Context,
//        filmFactsRepository: FilmFactsRepository,
//        recentPromptsRepository: RecentPromptsRepository,
//        userDataRepository: UserDataRepository
//    ) = GetFloppingMoviesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository)

    @Provides
    fun provideMoviesStarringActorUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository
    ) = GetMoviesStarringActorUseCase(context, filmFactsRepository, recentPromptsRepository)

    @Provides
    fun provideScoredMoviesStarringActorUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository
    ) = GetScoredMoviesStarringActorUseCase(context, filmFactsRepository, recentPromptsRepository)

    @Provides
    fun provideBiggestFilmographyUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
    ) = GetBiggestFilmographyUseCase(context, filmFactsRepository, recentPromptsRepository)

    @Provides
    fun provideEarliestFilmographyUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        calendarProvider: CalendarProvider
    ) = GetEarliestFilmographyUseCase(context, filmFactsRepository, recentPromptsRepository, calendarProvider)

    @Provides
    fun provideMovieActorRolesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository
    ) = GetMovieActorRolesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository)

    @Provides
    fun provideVoiceActorMovieRolesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository
    ) = GetVoiceActorMovieRolesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository)

    @Provides
    fun provideMovieImageUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetMovieImageUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideMovieGenreImageUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        @Named(MOVIE_GENRE_IMAGES) genreImageRepository: GenreImageRepository
    ) = GetMovieGenreImagesUseCase(context, filmFactsRepository, genreImageRepository)

    @Provides
    fun provideAwardAchievementsUseCase(
        calendarProvider: CalendarProvider,
        userProgressRepository: UserProgressRepository,
        userDataRepository: UserDataRepository
    ): AwardAchievementsUseCase = AwardAchievementsUseCase(calendarProvider, userProgressRepository, userDataRepository)

    @Provides
    fun provideLongestRunningTvShowUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ): GetLongestRunningTvShowUseCase = GetLongestRunningTvShowUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideTvShowImageUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetTvShowImageUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideRatedTvShowSeasonUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetRatedTvShowSeasonUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideEarliestAiringTvShowUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetEarliestAiringTvShowUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideVoiceActorTvShowRolesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetVoiceActorTvShowRolesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideTvShowsStarringActorUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetTvShowsStarringActorUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideTvShowActorRolesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetTvShowActorRolesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideTvShowActorLongestRunningCharacterUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetTvShowActorLongestRunningCharacterUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideTvShowGenreImageUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        @Named(TV_GENRE_IMAGES) genreImageRepository: GenreImageRepository
    ) = GetTvShowGenreImagesUseCase(context, filmFactsRepository, genreImageRepository)

    @Provides
    @Singleton
    fun provideUiPromptController(
        recentPromptsRepository: RecentPromptsRepository,
        voiceActorMovieRolesUseCase: GetVoiceActorMovieRolesUseCase,
        topGrossingMoviesUseCase: GetTopGrossingMoviesUseCase,
        moviesStarringActorUseCase: GetMoviesStarringActorUseCase,
        scoredMoviesStarringActorUseCase: GetScoredMoviesStarringActorUseCase,
        biggestFilmographyUseCase: GetBiggestFilmographyUseCase,
        earliestFilmographyUseCase: GetEarliestFilmographyUseCase,
        actorRolesUseCase: GetMovieActorRolesUseCase,
        movieImageUseCase: GetMovieImageUseCase,
        longestRunningTvShowUseCase: GetLongestRunningTvShowUseCase,
        tvShowImageUseCase: GetTvShowImageUseCase,
        ratedTvShowSeasonUseCase: GetRatedTvShowSeasonUseCase,
        earliestAiringTvShowUseCase: GetEarliestAiringTvShowUseCase,
        voiceActorTvShowRolesUseCase: GetVoiceActorTvShowRolesUseCase,
        tvShowsStarringActorUseCase: GetTvShowsStarringActorUseCase,
        tvShowActorRolesUseCase: GetTvShowActorRolesUseCase,
        tvShowActorLongestRunningCharacterUseCase: GetTvShowActorLongestRunningCharacterUseCase
    ) = UiPromptController(
        recentPromptsRepository,
        voiceActorMovieRolesUseCase,
        topGrossingMoviesUseCase,
        moviesStarringActorUseCase,
        scoredMoviesStarringActorUseCase,
        biggestFilmographyUseCase,
        earliestFilmographyUseCase,
        actorRolesUseCase,
        movieImageUseCase,
        longestRunningTvShowUseCase,
        tvShowImageUseCase,
        ratedTvShowSeasonUseCase,
        earliestAiringTvShowUseCase,
        voiceActorTvShowRolesUseCase,
        tvShowsStarringActorUseCase,
        tvShowActorRolesUseCase,
        tvShowActorLongestRunningCharacterUseCase
    )

    private companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val RECENT_ACTORS = "RECENT_ACTORS"
        const val RECENT_MOVIES = "RECENT_MOVIES"
        const val RECENT_TV_SHOWS = "RECENT_TV_SHOWS"
        const val MOVIE_GENRE_IMAGES = "MOVIE_GENRE_IMAGES"
        const val TV_GENRE_IMAGES = "TV_GENRE_IMAGES"
        const val MOVIE_SETTINGS_KEY = "SETTINGS_KEY"
        const val TV_SHOW_SETTINGS_KEY = "TV_SHOW_SETTINGS_KEY"
    }
}