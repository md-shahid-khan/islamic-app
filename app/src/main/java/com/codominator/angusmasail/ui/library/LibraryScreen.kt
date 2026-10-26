package com.codominator.angusmasail.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.ApiClient
import com.codominator.angusmasail.network.ImamResourceDto
import com.codominator.angusmasail.ui.chat.downloadAttachment
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val books by viewModel.libraryBooks.collectAsState()
    val isLoading by viewModel.isLoadingLibrary.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canUpload = userRole == "ROLE_IMAM" || userRole == "ROLE_ADMIN" || currentUser?.isImamVerified == true || currentUser?.role == "ROLE_IMAM"

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("All", "Quran", "Hadith", "Aqaid", "Namaz", "Masail")

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<com.codominator.angusmasail.network.ImamResourceDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchLibraryBooks()
    }

    val filteredBooks = remember(selectedCategoryIndex, searchQuery, books) {
        val selectedCat = categories[selectedCategoryIndex].uppercase()
        books.filter { b ->
            val desc = b.description ?: ""
            val title = b.title ?: ""
            val categoryMatches = if (selectedCat == "ALL") {
                true
            } else {
                desc.uppercase().contains("[$selectedCat]") || desc.uppercase().contains(selectedCat) || title.uppercase().contains(selectedCat)
            }

            val searchMatches = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                title.lowercase().contains(q) || desc.lowercase().contains(q)
            }

            categoryMatches && searchMatches
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Islamic Public Library", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepEmerald)
                        Text("Free Books & PDF Resources for All Congregants", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DeepEmerald)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                    if (canUpload) {
                        IconButton(onClick = { showUploadDialog = true }) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = "Upload Book", tint = DeepEmerald)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmAlabaster)
            )
        },
        floatingActionButton = {
            if (canUpload) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = DeepEmerald,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 76.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Book", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
        ) {
            // 1. Search Bar (if toggled)
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, author, or keyword...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PureCream,
                        unfocusedContainerColor = PureCream,
                        focusedBorderColor = DeepEmerald,
                        unfocusedBorderColor = Color(0xFFE2D8C7)
                    )
                )
            }

            // 2. Category Filter Tabs: All, Quran, Hadith, Aqaid, Namaz, Masail
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = WarmAlabaster,
                contentColor = DeepEmerald,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = Color(0xFFEFE8DC)) }
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // 3. Books List
            if (isLoading && books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DeepEmerald)
                }
            } else if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No books found in this section", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Imams can upload PDF resources here for all congregants to read.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        LibraryBookCard(
                            book = book,
                            canDelete = userRole == "ROLE_ADMIN",
                            onDelete = { bookToDelete = book },
                            onDownload = {
                                val url = book.fileUrl
                                if (!url.isNullOrBlank()) {
                                    downloadAttachment(context, url, "${book.title}.pdf")
                                } else {
                                    Toast.makeText(context, "No PDF link attached to this book", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReadOnline = {
                                val url = book.fileUrl
                                if (!url.isNullOrBlank()) {
                                    val resolved = ApiClient.resolveMediaUrl(url) ?: url
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resolved))
                                    try {
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        downloadAttachment(context, url, "${book.title}.pdf")
                                    }
                                } else {
                                    Toast.makeText(context, "Reading content online...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadBookDialog(
            viewModel = viewModel,
            onDismiss = { showUploadDialog = false },
            onSuccess = {
                showUploadDialog = false
                viewModel.fetchLibraryBooks()
                Toast.makeText(context, "Book uploaded successfully to public library!", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (bookToDelete != null) {
        val target = bookToDelete!!
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            icon = {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color(0xFFC0392B), modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Delete Book from Library?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${target.title}\"?\n\nThis will remove the book and its PDF file from the public Islamic library for all users.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        bookToDelete = null
                        viewModel.deleteBookResource(id) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Book deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "Deletion failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun LibraryBookCard(
    book: ImamResourceDto,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {},
    onDownload: () -> Unit,
    onReadOnline: () -> Unit
) {
    val categoryTag = remember(book.description) {
        val desc = book.description ?: ""
        when {
            desc.contains("[QURAN]") -> "QURAN" to DeepEmerald
            desc.contains("[HADITH]") -> "HADITH" to Color(0xFF6B46C1)
            desc.contains("[AQAID]") -> "AQAID" to Color(0xFFB78103)
            desc.contains("[NAMAZ]") -> "NAMAZ" to Color(0xFF1976D2)
            desc.contains("[MASAIL]") -> "MASAIL" to Color(0xFFC0392B)
            else -> "ISLAMIC" to ForestSlate
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE5DACB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge Icon Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(categoryTag.second.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            tint = categoryTag.second,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = categoryTag.first,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = categoryTag.second
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // PDF pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = "PDF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (canDelete) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Book",
                                tint = Color(0xFFC0392B),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            if (!book.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = book.description.removePrefix("[${categoryTag.first}]").trim(),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEFE8DC))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Read Online & Download Book
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReadOnline,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald),
                    border = BorderStroke(1.dp, DeepEmerald)
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadBookDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("MASAIL") }
    var description by remember { mutableStateOf("") }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val categories = listOf("QURAN", "HADITH", "AQAID", "NAMAZ", "MASAIL")

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            var fileName = "book.pdf"
            var fileSize = -1L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }
            if (fileSize > 15 * 1024 * 1024L) {
                Toast.makeText(context, "PDF file exceeds 15 MB limit.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            selectedFileUri = uri
            selectedFileName = fileName
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = DeepEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Public Book / PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "All congregants will be able to read and download this resource publicly.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book Title *") },
                    placeholder = { Text("e.g. Bahar-e-Shariat") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author / Scholar") },
                    placeholder = { Text("e.g. Sadrush Shariah Allama Amjad Ali") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category selector
                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepEmerald,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Summary") },
                    placeholder = { Text("Key topics covered in this book...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                // PDF file attachment selector
                Surface(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    shape = RoundedCornerShape(12.dp),
                    color = BgMintTint,
                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = DeepEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedFileName ?: "Select PDF Book File (Max 15 MB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a book title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isUploading = true
                    val uri = selectedFileUri
                    if (uri != null) {
                        viewModel.uploadChatAttachment(uri) { success, resp, err ->
                            if (success && resp != null) {
                                viewModel.uploadLibraryBook(
                                    title = title.trim(),
                                    author = author.trim(),
                                    category = selectedCategory,
                                    description = description.trim(),
                                    fileUrl = resp.url
                                ) { ok, uploadErr ->
                                    isUploading = false
                                    if (ok) onSuccess() else Toast.makeText(context, uploadErr ?: "Failed", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isUploading = false
                                Toast.makeText(context, err ?: "PDF upload failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        // Direct entry without attachment
                        viewModel.uploadLibraryBook(
                            title = title.trim(),
                            author = author.trim(),
                            category = selectedCategory,
                            description = description.trim(),
                            fileUrl = ""
                        ) { ok, uploadErr ->
                            isUploading = false
                            if (ok) onSuccess() else Toast.makeText(context, uploadErr ?: "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isUploading && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Publish to Library")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Cancel")
            }
        },
        containerColor = PureCream,
        shape = RoundedCornerShape(20.dp)
    )
}
