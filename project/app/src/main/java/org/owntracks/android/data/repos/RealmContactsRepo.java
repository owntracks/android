package org.owntracks.android.data.repos;

import android.annotation.SuppressLint;

import org.owntracks.android.injection.scopes.PerApplication;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
@PerApplication
@SuppressLint("NewApi") // try-with-resources is backported by retrolambda
public class RealmContactsRepo implements ContactsRepo {

   // private final Provider<Realm> realmProvider;
//
   // @Inject
   // public RealmContactsRepo(Provider<Realm> realmProvider) {
   //     this.realmProvider = realmProvider;
   // }
//
   // @Override
   // public List<Country> findAllSorted(String sortField, Sort sort, boolean detached) {
   //     try(Realm realm = realmProvider.get()) {
   //         RealmResults<Country> realmResults = realm.where(Country.class).findAllSorted(sortField, sort);
//
   //         if(detached) {
   //             return realm.copyFromRealm(realmResults);
   //         } else {
   //             return realmResults;
   //         }
   //     }
   // }
//
   // @Override
   // public RealmResults<Country> findAllSortedWithListener(String sortField, Sort sort, RealmChangeListener<RealmResults<Country>> listener) {
   //     try(Realm realm = realmProvider.get()) {
   //         RealmResults<Country> realmResults = realm.where(Country.class).findAllSorted(sortField, sort);
   //         realmResults.addChangeListener(listener);
   //         return realmResults;
   //     }
   // }
//
   // @Override
   // @Nullable
   // public Country getByField(String field, String value, boolean detached) {
   //     try(Realm realm = realmProvider.get()) {
   //         Country realmCountry = realm.where(Country.class).equalTo(field, value).findFirst();
   //         if(detached && realmCountry != null) { realmCountry = realm.copyFromRealm(realmCountry); }
   //         return realmCountry;
   //     }
   // }
//
   // @Override
   // public void update(Country country) {
   //     try(Realm realm = realmProvider.get()) {
   //         realm.executeTransaction(r -> r.copyToRealmOrUpdate(country));
   //     }
   // }
//
   // @Override
   // public void delete(Country realmCountry) {
   //     if(realmCountry.isValid()) {
   //         try (Realm realm = realmProvider.get()) {
   //             realm.executeTransaction(r -> {
   //                 realmCountry.borders.deleteAllFromRealm();
   //                 realmCountry.currencies.deleteAllFromRealm();
   //                 realmCountry.languages.deleteAllFromRealm();
   //                 realmCountry.translations.deleteAllFromRealm();
   //                 realmCountry.deleteFromRealm();
   //             });
   //         }
   //     }
   // }
//
   // @Override
   // public Country detach(Country country) {
   //     if(country.isValid()) {
   //         try(Realm realm = realmProvider.get()) {
   //             return realm.copyFromRealm(country);
   //         }
   //     } else {
   //         return country;
   //     }
   // }
}
