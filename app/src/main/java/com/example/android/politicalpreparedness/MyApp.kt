package com.example.android.politicalpreparedness

import android.app.Application
import com.example.android.politicalpreparedness.database.ElectionDataSource
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.database.ElectionRepository
import com.example.android.politicalpreparedness.database.LocalDB
import com.example.android.politicalpreparedness.election.ElectionsViewModel
import com.example.android.politicalpreparedness.election.VoterInfoViewModel
import com.example.android.politicalpreparedness.representative.RepresentativeViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        /**
         * use Koin Library as a service locator for dependency injection.
         * A service locator is a design pattern that provides a registry of available dependencies and
         * creates instances of these dependencies on demand.
         */
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                ElectionsViewModel(
                    get(),
                    get() as ElectionDataSource,
                    get() as ElectionRepository
                )
            }
            viewModel {
                VoterInfoViewModel(get(), get() as ElectionRepository)
            }
            viewModel {
                RepresentativeViewModel(get(), get() as ElectionRepository)
            }

            // Declare singleton definitions to be later injected using by inject()
            single { ElectionDatabase.getDatabase(this.androidContext()) }
            single<ElectionDataSource> {
                ElectionRepository(electionDao = get(), ioDispatcher = Dispatchers.IO)
            }
            single <ElectionRepository> { ElectionRepository(electionDao = get(), ioDispatcher = Dispatchers.IO) }
            single { LocalDB.createElectionDao(this@MyApp) }
        }

        startKoin {
            androidContext(this@MyApp)
            modules(listOf(myModule))
        }
    }
}