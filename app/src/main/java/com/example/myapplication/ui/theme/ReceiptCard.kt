package com.example.myapplication

import androidx.compose.runtime.Composable
import com.example.myapplication.data.ReceiptEntity
import com.example.myapplication.data.RetailerTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.ReceiptParser
import com.example.myapplication.data.displayDetails
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox



@Composable
fun ReceiptCard(
    receipt: ReceiptEntity,
    theme: RetailerTheme,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit

)
{
    val showDeleteDialog = remember {
        mutableStateOf(false)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = theme.accentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (selectionMode) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            onSelectionChange(checked)
                        }
                    )

                    Text(
                        text = if (selected) "Selected" else "Select"
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Text(
                text = theme.retailerName,
                style = MaterialTheme.typography.headlineSmall,
                color = theme.headerColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅",
                    color = theme.headerColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = receipt.receiptDate ?: "Unknown"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🛒")

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Items: ${
                            ReceiptParser.extractItemCount(receipt.rawText)
                                ?: ReceiptParser.extractProducts(receipt.rawText).size
                        }"
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💰",
                        color = theme.headerColor
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text =
                            receipt.totalAmount
                                ?.let { "$%.2f".format(it) }
                                ?: "-"
                    )
                }
            }

            Text(
                text =
                    if (expanded)
                        "▲ Hide ${
                            ReceiptParser.extractItemCount(receipt.rawText)
                                ?: ReceiptParser.extractProducts(receipt.rawText).size
                        } products"
                    else
                        "▼ ${
                            ReceiptParser.extractItemCount(receipt.rawText)
                                ?: ReceiptParser.extractProducts(receipt.rawText).size
                        } products recognised",
                color = theme.headerColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable {
                        onExpandToggle()
                    }
                    .padding(top = 8.dp)
                    .align(Alignment.End)
            )
            if (expanded) {

                Spacer(modifier = Modifier.height(12.dp))

                val parsed =
                    ReceiptParser.parse(receipt.rawText)

                if (parsed.structuredItems.isNotEmpty()) {

                    parsed.structuredItems.forEach { item ->

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {

                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(
                                modifier = Modifier.height(2.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text(
                                    text = item.displayDetails(),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Text(
                                    text =
                                        item.totalPrice
                                            ?.let {
                                                "$%.2f".format(it)
                                            }
                                            ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                } else {

                    parsed.products.forEach { product ->

                        Text(
                            text = "• $product",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Delete receipt",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clickable {
                            onDelete()
                        }
                        .align(Alignment.End)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Delete receipt",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clickable {
                            showDeleteDialog.value = true
                        }
                        .align(Alignment.End)
                )
                if (showDeleteDialog.value) {

                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog.value = false
                        },
                        title = {
                            Text("Delete Receipt")
                        },
                        text = {
                            Text(
                                "Delete this receipt and its associated purchase history?"
                            )
                        },
                        confirmButton = {

                            Button(
                                onClick = {

                                    showDeleteDialog.value = false

                                    onDelete()
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {

                            Button(
                                onClick = {
                                    showDeleteDialog.value = false
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            }
        }
    }

