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
        tooManyRequestsDataSource: TooManyRequestsDataSource
    ) = TmdbDataSource(discoverService, configurationService, personService, movieService, tooManyRequestsDataSource)

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
    fun provideUserSettingsDataSource(
        @ApplicationContext context: Context
    ): UserSettingsDataSource = UserSettingsDataSource(context)

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
    ): RecentElementsDataSource = RecentElementsDataSource(context, "RECENT_ACTORS")

    @Singleton
    @Provides
    @Named(RECENT_MOVIES)
    fun provideRecentMoviesDataSource(
        @ApplicationContext context: Context
    ): RecentElementsDataSource = RecentElementsDataSource(context, "RECENT_MOVIES")

    @Singleton
    @Provides
    fun provideRecentFlopsDataSource(): RecentFlopsDataSource = RecentFlopsDataSource()

    @Singleton
    @Provides
    fun provideGenreImageDataSource(
        @ApplicationContext context: Context
    ): GenreImageDataSource = GenreImageDataSource(context)

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
        @Named(RECENT_MOVIES) recentMoviesDataSource: RecentElementsDataSource
    ) = RecentPromptsRepository(recentActorsDataSource, recentFlopsDataSource, recentMoviesDataSource)

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
        userSettingsDataSource: UserSettingsDataSource,
        sessionDataSource: SessionDataSource,
        accountDataSource: AccountDataSource
    ) = UserDataRepository(
        context,
        authenticationRepository,
        userSettingsDataSource,
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
    fun provideGenreImageRepository(
        genreImageDataSource: GenreImageDataSource
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
    ) = GetEarliestFilmographyUseCase(context, filmFactsRepository, recentPromptsRepository)

    @Provides
    fun provideActorRolesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository
    ) = GetActorRolesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository)

    @Provides
    fun provideVoiceActorRolesUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository
    ) = GetVoiceActorRolesUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository)

    @Provides
    fun provideMovieImageUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        recentPromptsRepository: RecentPromptsRepository,
        userDataRepository: UserDataRepository,
        calendarProvider: CalendarProvider
    ) = GetMovieImageUseCase(context, filmFactsRepository, recentPromptsRepository, userDataRepository, calendarProvider)

    @Provides
    fun provideGenreImageUseCase(
        @ApplicationContext context: Context,
        filmFactsRepository: FilmFactsRepository,
        genreImageRepository: GenreImageRepository
    ) = GetGenreImagesUseCase(context, filmFactsRepository, genreImageRepository)

    @Provides
    fun provideAwardAchievementsUseCase(
        calendarProvider: CalendarProvider,
        userProgressRepository: UserProgressRepository,
        userDataRepository: UserDataRepository
    ): AwardAchievementsUseCase = AwardAchievementsUseCase(calendarProvider, userProgressRepository, userDataRepository)

    @Provides
    @Singleton
    fun provideUiPromptController(
        recentPromptsRepository: RecentPromptsRepository,
        voiceActorRolesUseCase: GetVoiceActorRolesUseCase,
        topGrossingMoviesUseCase: GetTopGrossingMoviesUseCase,
        moviesStarringActorUseCase: GetMoviesStarringActorUseCase,
        scoredMoviesStarringActorUseCase: GetScoredMoviesStarringActorUseCase,
        biggestFilmographyUseCase: GetBiggestFilmographyUseCase,
        earliestFilmographyUseCase: GetEarliestFilmographyUseCase,
        actorRolesUseCase: GetActorRolesUseCase,
        movieImageUseCase: GetMovieImageUseCase
    ) = UiPromptController(
        recentPromptsRepository,
        voiceActorRolesUseCase,
        topGrossingMoviesUseCase,
        moviesStarringActorUseCase,
        scoredMoviesStarringActorUseCase,
        biggestFilmographyUseCase,
        earliestFilmographyUseCase,
        actorRolesUseCase,
        movieImageUseCase
    )

    private companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val RECENT_ACTORS = "RECENT_ACTORS"
        const val RECENT_MOVIES = "RECENT_MOVIES"
    }
}