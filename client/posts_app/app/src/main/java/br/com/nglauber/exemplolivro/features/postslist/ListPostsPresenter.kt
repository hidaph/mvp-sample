package br.com.nglauber.exemplolivro.features.postslist

import br.com.nglauber.exemplolivro.App
import br.com.nglauber.exemplolivro.model.persistence.PostDataSource
import br.com.nglauber.exemplolivro.shared.binding.PostBinding
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class ListPostsPresenter @Inject constructor(
        var dataSource: PostDataSource,
        val subscriberScheduler : Scheduler = Schedulers.io(),
        val observerScheduler : Scheduler = AndroidSchedulers.mainThread())
    : ListPostsContract.Presenter {

    private lateinit var view: ListPostsContract.View
    private val subscriptions = CompositeDisposable()
    private var listPosts : MutableList<PostBinding>? = null

    init {
        App.instance.component.inject(this)
    }

    override fun loadPosts(ignoreCache: Boolean) {
        val postsBindingList = ArrayList<PostBinding>()
        view.showProgress(true)

        subscriptions.clear()
        if (ignoreCache) listPosts = null

        val listPostsObs : Observable<PostBinding>
        if (ignoreCache || listPosts == null){
            listPosts = mutableListOf<PostBinding>()
            listPostsObs = dataSource.loadPosts()
                    .map(::PostBinding)
                    .doOnNext { listPosts?.add(it) }
        } else {
            listPostsObs = Observable.fromIterable(listPosts)
        }
        val subscr = listPostsObs
                .subscribeOn(subscriberScheduler)
                .observeOn(observerScheduler)
                .subscribe(
                        { postBinding ->
                            postsBindingList.add(postBinding)
                        },
                        { throwable ->
                            Timber.e(throwable)
                            view.showProgress(false)
                            view.showLoadErrorMessage()
                        },
                        {
                            view.updateList(postsBindingList)
                            view.showProgress(false)
                            view.showEmptyView(postsBindingList.size == 0)
                        }
                )
        subscriptions.add(subscr)
    }

    override fun subscribe() {
        loadPosts(false)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun editPost(postId: Long) {
        view.editPost(postId)
    }

    override fun addNewPost() {
        view.addNewPost()
    }

    override fun attachView(view: ListPostsContract.View) {
        this.view = view
    }
}
