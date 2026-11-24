package com.homelab.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lan
import com.homelab.app.R
import com.homelab.app.domain.model.ServiceInstance
import com.homelab.app.util.ServiceType

/**
 * Opens the login screen to add a new instance of the given service type. Provided once by the
 * navigation host so every dashboard picker offers the action without extra wiring.
 */
val LocalAddServiceInstance = staticCompositionLocalOf<((ServiceType) -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceInstancePicker(
    instances: List<ServiceInstance>,
    selectedInstanceId: String,
    onInstanceSelected: (ServiceInstance) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    if (instances.isEmpty()) return
    val onAddInstance = LocalAddServiceInstance.current
    val serviceType = instances.first().type
    if (instances.size == 1) {
        if (onAddInstance != null) AddInstanceButton(serviceType, onAddInstance, modifier)
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val selectedInstance = instances.firstOrNull { it.id == selectedInstanceId } ?: instances.first()
    val resolvedLabel = label ?: stringResource(R.string.service_instance_label)

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            OutlinedTextField(
                value = selectedInstance.label.ifBlank { selectedInstance.type.displayName },
                onValueChange = {},
                readOnly = true,
                label = { Text(resolvedLabel) },
                supportingText = {
                    Text(
                        text = selectedInstance.url,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lan,
                        contentDescription = resolvedLabel,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                instances.forEach { instance ->
                    DropdownMenuItem(
                        text = {
                            Row {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = instance.label.ifBlank { instance.type.displayName },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (instance.id == selectedInstanceId) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = instance.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (instance.id == selectedInstanceId) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.service_instance_active),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onInstanceSelected(instance)
                        }
                    )
                }
                if (onAddInstance != null) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.service_instance_add_another, serviceType.displayName),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            expanded = false
                            onAddInstance(serviceType)
                        }
                    )
                }
            }
        }
    }
}

/** Shown when a single instance exists, so adding a second one is discoverable from the dashboard. */
@Composable
private fun AddInstanceButton(
    serviceType: ServiceType,
    onAddInstance: (ServiceType) -> Unit,
    modifier: Modifier
) {
    TextButton(
        onClick = { onAddInstance(serviceType) },
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.service_instance_add_another, serviceType.displayName))
    }
}
