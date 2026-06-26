package ir.mtnmh.primeaccount.listings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.components.PrimeButton
import ir.mtnmh.primeaccount.core.components.PrimeToolbar
import ir.mtnmh.primeaccount.utils.AppLanguage
import ir.mtnmh.primeaccount.utils.LocalLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    viewModel: ListingsViewModel,
    onSuccessPublish: () -> Unit,
    isGuest: Boolean = false,
    currentUserStatus: String = "Approved",
    onLoginRequired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    val isRtl = languageManager.currentLanguageFlow == AppLanguage.PERSIAN
    val context = LocalContext.current

    if (isGuest) {
        Scaffold(
            topBar = {
                PrimeToolbar(title = stringResource(id = R.string.create_listing_title))
            },
            modifier = modifier.testTag("create_listing_screen")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isRtl) "ورود به حساب کاربری لازم است" else "Login Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRtl) "تنها کاربران ثبت‌نام شده و تایید شده می‌توانند آگهی جدید ثبت کنند." 
                               else "Only logged-in and approved users can publish new listings. Please log in or register to proceed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onLoginRequired,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
                    ) {
                        Text(text = if (isRtl) "ورود / ثبت‌نام" else "Log In / Register")
                    }
                }
            }
        }
        return
    }

    val createGame by viewModel.createGame.collectAsState()
    val createTitle by viewModel.createTitle.collectAsState()
    val createDescription by viewModel.createDescription.collectAsState()
    val createPrice by viewModel.createPrice.collectAsState()
    val createPlatform by viewModel.createPlatform.collectAsState()
    val createAccountLevel by viewModel.createAccountLevel.collectAsState()
    val createCoins by viewModel.createCoins.collectAsState()
    val createSpecialPlayers by viewModel.createSpecialPlayers.collectAsState()
    val createImages by viewModel.createImages.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val createError by viewModel.createError.collectAsState()
    val createSuccess by viewModel.createSuccess.collectAsState()

    val gamesList = listOf("EA FC Mobile", "eFootball (PES Mobile)")
    val platformsList = listOf("Android", "iOS", "Both")

    var showImageSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        uris.forEach { uri ->
            val validation = ir.mtnmh.primeaccount.utils.ImageValidator.validateImage(context, uri.toString())
            if (validation.isValid) {
                viewModel.addImageToForm(uri.toString())
            } else {
                Toast.makeText(context, validation.reason, Toast.LENGTH_LONG).show()
            }
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val cacheFile = java.io.File(context.cacheDir, "camera_image_${System.currentTimeMillis()}.jpg")
                val out = java.io.FileOutputStream(cacheFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                val fileUri = android.net.Uri.fromFile(cacheFile).toString()
                val validation = ir.mtnmh.primeaccount.utils.ImageValidator.validateImage(context, fileUri)
                if (validation.isValid) {
                    viewModel.addImageToForm(fileUri)
                } else {
                    Toast.makeText(context, validation.reason, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val requestPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        galleryLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    LaunchedEffect(createSuccess) {
        if (createSuccess) {
            Toast.makeText(
                context,
                if (isRtl) "آگهی شما با موفقیت منتشر شد!" else "Your listing has been successfully published!",
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetSuccess()
            onSuccessPublish()
        }
    }

    Scaffold(
        topBar = {
            PrimeToolbar(
                title = if (isRtl) "ثبت آگهی فروش" else "Post a New Listing"
            )
        },
        modifier = modifier.testTag("create_listing_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Form Header Description
            Text(
                text = if (isRtl) "مشخصات کامل اکانت خود را برای فروش در بازار ثبت کنید." 
                       else "Verify your account info to list it safely in the marketplace.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Choose supported Game
            Text(
                text = if (isRtl) "انتخاب بازی *" else "Select Game *",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gamesList.forEach { gameItem ->
                    val isSelected = createGame == gameItem
                    val isEa = gameItem.contains("EA")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                color = if (isSelected) {
                                    if (isEa) Color(0xFF1565C0) else Color(0xFFFFD54F)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    if (isEa) Color(0xFF0D47A1) else Color(0xFFFBC02D)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.createGame.value = gameItem }
                            .testTag("form_game_select_${gameItem.replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gameItem,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    if (isEa) Color.White else Color(0xFF1565C0)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        )
                    }
                }
            }

            // 2. Title Form Field
            OutlinedTextField(
                value = createTitle,
                onValueChange = { viewModel.createTitle.value = it },
                label = { Text(if (isRtl) "عنوان آگهی *" else "Listing Title *") },
                placeholder = { Text(if (isRtl) "مثلا: اکانت لول ۸۰ با ۵۰ میلیون کوین" else "e.g., OVR 104 Prime Account") },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                ),
                modifier = Modifier.fillMaxWidth().testTag("form_input_title")
            )

            // 3. Description Form Field
            OutlinedTextField(
                value = createDescription,
                onValueChange = { viewModel.createDescription.value = it },
                label = { Text(if (isRtl) "توضیحات کامل آگهی *" else "Description *") },
                placeholder = { Text(if (isRtl) "جزئیات بازیکنان خاص، سابقه، روش انتقال و..." else "List specific features, link details etc.") },
                minLines = 3,
                maxLines = 5,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                ),
                modifier = Modifier.fillMaxWidth().testTag("form_input_desc")
            )

            // 4. Price & Price Category display helper
            Column {
                OutlinedTextField(
                    value = createPrice,
                    onValueChange = { viewModel.createPrice.value = it },
                    label = { Text(if (isRtl) "مبلغ اکانت (تومان) *" else "Price (Toman) *") },
                    placeholder = { Text("1500000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("form_input_price")
                )
                
                // Real-time calculated price category preview banner
                val enteredPrice = createPrice.toDoubleOrNull()
                if (enteredPrice != null && enteredPrice > 0) {
                    val cat = Listing.computePriceCategory(enteredPrice)
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isRtl) "دسته‌بندی قیمت خودکار: ${translatePriceCategory(cat)}" 
                                   else "Auto-assigned Bracket: $cat",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 5. Choose Platform Selector
            Text(
                text = if (isRtl) "پلتفرم اکانت" else "Account Platform",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                platformsList.forEach { platform ->
                    val isSelected = createPlatform == platform
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.createPlatform.value = platform },
                        label = { Text(platform) },
                        modifier = Modifier.weight(1f).testTag("form_platform_${platform}")
                    )
                }
            }

            // 6. Account specifications (Details)
            Text(
                text = if (isRtl) "مشخصات فنی اکانت" else "Technical Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = createAccountLevel,
                    onValueChange = { viewModel.createAccountLevel.value = it },
                    label = { Text(if (isRtl) "سطح (لول)" else "ACC Level") },
                    placeholder = { Text("75") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                    ),
                    modifier = Modifier.weight(1f).testTag("form_spec_level")
                )

                OutlinedTextField(
                    value = createCoins,
                    onValueChange = { viewModel.createCoins.value = it },
                    label = { Text(if (isRtl) "موجودی کوین (یا پوینت)" else "Total Coins") },
                    placeholder = { Text("12.5M") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                    ),
                    modifier = Modifier.weight(1.2f).testTag("form_spec_coins")
                )
            }

            // Special Players Text input
            OutlinedTextField(
                value = createSpecialPlayers,
                onValueChange = { viewModel.createSpecialPlayers.value = it },
                label = { Text(if (isRtl) "بازیکنان خاص و ویژه" else "Special/Legendary Players") },
                placeholder = { Text(if (isRtl) "مثلا: مسی ۱۰۵، رونالدو آر۹، صید اول" else "e.g., Epic Messi (Maxed), Prime Gullit") },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                ),
                modifier = Modifier.fillMaxWidth().testTag("form_spec_players")
            )

            // 7. Image selection (Up to 6 images upload preparation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRtl) "بارگذاری نقشه/تصاویر اکانت (${createImages.size}/۶)" 
                           else "Upload Images (${createImages.size}/6)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (createImages.size < 6) {
                    TextButton(onClick = {
                        showImageSourceDialog = true
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRtl) "افزودن تصویر" else "Add Photo")
                    }
                }
            }

            // Image Carousel Preview Grid
            if (createImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(createImages) { index, imgUri ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imgUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Upload index $index",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Cross delete round button over image
                            IconButton(
                                onClick = { viewModel.removeImageFromForm(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.White.copy(alpha = 0.9f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Photo",
                                    tint = Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "هیچ تصویری انتخاب نشده است (حداکثر ۶ تصویر)" 
                               else "No images selected yet (up to 6 images).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Error display block
            createError?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.error, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            PrimeButton(
                text = if (isRtl) "انتشار آگهی" else "Publish Listing",
                onClick = { viewModel.publishListing() },
                isLoading = isPublishing,
                modifier = Modifier.fillMaxWidth().testTag("publish_listing_button")
            )
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    text = if (isRtl) "انتخاب منبع تصویر" else "Select Image Source",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = {
                            showImageSourceDialog = false
                            val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
                            } else {
                                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            requestPermissionLauncher.launch(permissionsToRequest)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(if (isRtl) "گالری تصاویر" else "Photo Gallery")
                        }
                    }

                    Card(
                        onClick = {
                            showImageSourceDialog = false
                            cameraLauncher.launch(null)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(if (isRtl) "دوربین عکاسی" else "Camera")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text(if (isRtl) "انصراف" else "Cancel")
                }
            }
        )
    }
}
