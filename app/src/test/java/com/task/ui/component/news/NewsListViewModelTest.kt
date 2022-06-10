package com.task.ui.component.news

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.task.data.remote.dto.NewsModel
import com.task.ui.base.listeners.BaseCallback
import com.task.usecase.NewsUseCase
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class NewsListViewModelTest {
    // Subject under test
    private lateinit var newsListViewModel: NewsListViewModel

    // Use a fake UseCase to be injected into the viewmodel
    private val newsUseCase: NewsUseCase = mockk()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val newsTitle = "this is test"
    private val testModelsGenerator: TestModelsGenerator = TestModelsGenerator()

    @Before
    fun setUp() {
        // Create class under test
        // We initialise the repository with no tasks
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        newsListViewModel = NewsListViewModel(newsUseCase)
    }

    @Test
    fun getNewsList() {
        // Let's do a synchronous answer for the callback
        val newsModeltest = testModelsGenerator.generateNewsModel(newsTitle)
        //1- Mock - double test
        (newsListViewModel).apply {
            newsModel.value = newsModeltest
            newsSearchFound.value = testModelsGenerator.generateNewsItemModel(newsTitle)
            noSearchFound.value = false
        }
        val callbackCapture: CapturingSlot<BaseCallback> = slot()
        every { newsUseCase.getNews(callback = capture(callbackCapture)) } answers
                { callbackCapture.captured.onSuccess(newsModeltest) }
        //2-Call
        newsListViewModel.getNews()
        //3-verify
        assert(newsModeltest == newsListViewModel.newsModel.value)
    }

    @Test
    fun testSearchSuccess() {
        val newsItem = testModelsGenerator.generateNewsItemModel(newsTitle)
        val newsModel = testModelsGenerator.generateNewsModel(newsTitle)
        //1- Mock
        val callbackCapture: CapturingSlot<BaseCallback> = slot()
        every { newsUseCase.getNews(callback = capture(callbackCapture)) } answers
                { callbackCapture.captured.onSuccess(newsModel) }
        every { newsUseCase.searchByTitle(newsModel.newsItems!!, newsTitle) } returns newsItem
        //2- Call
        newsListViewModel.getNews()
        newsListViewModel.onSearchClick(newsTitle)
        //3- Verify
        assert(newsListViewModel.noSearchFound.value == false)
        assert(newsListViewModel.newsSearchFound.value == newsItem)
    }

    @Test
    fun testSearchFailedWhileEmptyList() {
        val newsModelWithEmptyList: NewsModel = testModelsGenerator.generateNewsModelWithEmptyList("stup")
        //1- Mock
        val callbackCapture: CapturingSlot<BaseCallback> = slot()
        every { newsUseCase.getNews(callback = capture(callbackCapture)) } answers
                { callbackCapture.captured.onSuccess(newsModelWithEmptyList) }
        every { newsUseCase.searchByTitle(newsModelWithEmptyList.newsItems!!, newsTitle) } returns null
        //2- Call
        newsListViewModel.getNews()
        newsListViewModel.onSearchClick(newsTitle)
        //3- Verify
        assert(newsListViewModel.noSearchFound.value == true)
        assert(newsListViewModel.newsSearchFound.value == null)
    }

    @Test
    fun testSearchFailedWhenNothingMatches() {
        val newsModel = testModelsGenerator.generateNewsModel(newsTitle)
        //1- Mock
        val callbackCapture: CapturingSlot<BaseCallback> = slot()
        every { newsUseCase.getNews(callback = capture(callbackCapture)) } answers
                { callbackCapture.captured.onSuccess(newsModel) }
        every { newsUseCase.searchByTitle(newsModel.newsItems!!, "*") } returns null
        //2- Call
        newsListViewModel.getNews()
        newsListViewModel.onSearchClick("*")
        //3- Verify
        assert(newsListViewModel.noSearchFound.value == true)
        assert(newsListViewModel.newsSearchFound.value == null)
    }

}