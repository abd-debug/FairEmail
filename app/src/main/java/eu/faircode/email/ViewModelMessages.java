package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2019 by Marcel Bokhorst (M66B)
*/

import java.util.HashMap;
import java.util.Map;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagedList;

public class ViewModelMessages extends ViewModel {
    private Map<Boolean, LiveData<PagedList<TupleMessageEx>>> messages = new HashMap<>();

    void setMessages(AdapterMessage.ViewType viewType, LifecycleOwner owner, final LiveData<PagedList<TupleMessageEx>> messages) {
        final boolean thread = (viewType == AdapterMessage.ViewType.THREAD);
        this.messages.put(thread, messages);

        // Keep list up-to-date for previous/next navigation
        messages.observe(owner, new Observer<PagedList<TupleMessageEx>>() {
            @Override
            public void onChanged(PagedList<TupleMessageEx> messages) {
            }
        });

        owner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroyed() {
                Log.i("Removed model thread=" + thread);
                ViewModelMessages.this.messages.remove(thread);
            }
        });
    }

    void observe(AdapterMessage.ViewType viewType, LifecycleOwner owner, Observer<PagedList<TupleMessageEx>> observer) {
        if (owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            final boolean thread = (viewType == AdapterMessage.ViewType.THREAD);
            messages.get(thread).observe(owner, observer);
        }
    }

    void removeObservers(AdapterMessage.ViewType viewType, LifecycleOwner owner) {
        if (owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            boolean thread = (viewType == AdapterMessage.ViewType.THREAD);
            LiveData<PagedList<TupleMessageEx>> list = messages.get(thread);
            if (list != null)
                list.removeObservers(owner);
        }
    }

    @Override
    protected void onCleared() {
        messages.clear();
    }

    void observePrevNext(LifecycleOwner owner, final String thread, final IPrevNext intf) {
        LiveData<PagedList<TupleMessageEx>> list = messages.get(false);
        if (list == null)
            return;

        list.observe(owner, new Observer<PagedList<TupleMessageEx>>() {
            @Override
            public void onChanged(PagedList<TupleMessageEx> list) {
                boolean load = false;
                for (int pos = 0; pos < list.size(); pos++) {
                    TupleMessageEx item = list.get(pos);
                    if (item != null && thread.equals(item.thread)) {
                        if (pos - 1 >= 0) {
                            TupleMessageEx next = list.get(pos - 1);
                            if (next == null)
                                load = true;
                            else
                                intf.onNext(next.id);
                        } else
                            intf.onNext(null);

                        if (pos + 1 < list.size()) {
                            TupleMessageEx prev = list.get(pos + 1);
                            if (prev == null)
                                load = true;
                            else
                                intf.onPrevious(prev.id);
                        } else
                            intf.onPrevious(null);

                        if (load)
                            list.loadAround(pos);

                        break;
                    }
                }
            }
        });
    }

    interface IPrevNext {
        void onPrevious(Long id);

        void onNext(Long id);
    }
}
