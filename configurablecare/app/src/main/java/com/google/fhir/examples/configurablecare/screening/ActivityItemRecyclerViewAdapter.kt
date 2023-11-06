/*
 * Copyright 2022-2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.fhir.examples.configurablecare.screening

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.fhir.examples.configurablecare.databinding.ItemActivityViewBinding

class ActivityItemRecyclerViewAdapter(
  private val onItemClicked: (ListScreeningsViewModel.ActivityItem) -> Unit
) :
  ListAdapter<ListScreeningsViewModel.ActivityItem, ActivityItemViewHolder>(
    TaskItemDiffCallback()
  ) {

  class TaskItemDiffCallback : DiffUtil.ItemCallback<ListScreeningsViewModel.ActivityItem>() {
    override fun areItemsTheSame(
      oldItem: ListScreeningsViewModel.ActivityItem,
      newItem: ListScreeningsViewModel.ActivityItem
    ) = oldItem.resourceId == newItem.resourceId

    override fun areContentsTheSame(
      oldItem: ListScreeningsViewModel.ActivityItem,
      newItem: ListScreeningsViewModel.ActivityItem
    ) = oldItem.id == newItem.id && oldItem.status == newItem.status
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityItemViewHolder {
    return ActivityItemViewHolder(
      ItemActivityViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
  }

  override fun onBindViewHolder(holder: ActivityItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item, onItemClicked)
  }
}
