package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.EmeraldSuccessDark
import com.example.ui.theme.OrangeSecondaryDark

@Composable
fun GuideScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("guide_header_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "APK ↔ ZIP Guide & Tips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Hindi & English Full Tutorial",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Text(
                    text = "APK aur ZIP ke beech conversion kaise kaam karta hai, sign kyu zaroori hota hai, aur best practices samjhein.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Section 1: What is APK and relationship with ZIP
        GuideTopicCard(
            icon = Icons.Default.Lightbulb,
            iconColor = MaterialTheme.colorScheme.primary,
            title = "1. APK aur ZIP me kya sambandh (relation) hai?",
            hindiBody = "Android APK asal me ek standard ZIP container hi hota hai! Iske andar application ka compiled Java/Kotlin code (classes.dex), resources (res), configuration (AndroidManifest.xml), assets aur signatures (META-INF) packed hote hain.\n\nAap kisi bhi APK ko ZIP me badal kar computer ya phone me kisi bhi ZIP extractor (WinRAR, 7-Zip, ZArchiver) se khol sakte hain.",
            englishBody = "An APK is fundamentally a specialized ZIP archive containing Android bytecode, resources, and manifest. Renaming or repackaging it to ZIP allows standard archive tools to extract and inspect its internal contents."
        )

        // Section 2: How to convert APK to ZIP
        GuideTopicCard(
            icon = Icons.Default.SwapHoriz,
            iconColor = OrangeSecondaryDark,
            title = "2. APK se ZIP kaise banayein?",
            hindiBody = "• Convert tab me 'APK → ZIP' mode select karein.\n• 'Tap to Select .APK File' par click karke apne phone se APK chunein, ya 'Installed Apps' tab se phone me maujood kisi bhi app ko direct pick karein.\n• Agar aap files modify karne wale hain, toh 'Strip Old Signatures' enable kar sakte hain.\n• 'Convert APK to ZIP' dabayein. File instantly ready ho jayegi jise aap Save ya Share kar sakte hain.",
            englishBody = "Select the APK, choose whether to remove old META-INF signatures, and tap convert. You can also directly pick from installed device applications."
        )

        // Section 3: Why Signing is critical for ZIP to APK
        GuideTopicCard(
            icon = Icons.Default.Security,
            iconColor = EmeraldSuccessDark,
            title = "3. ZIP se APK banane par Signature kyu zaroori hai?",
            hindiBody = "Bahut log puchte hain ki ZIP ko sirf .apk rename karne par app install kyu nahi hota ('App not installed' error kyu aata hai?).\n\nAndroid operating system bina valid digital signature ke kisi bhi APK ko install hone nahi deta. Jab aap ZIP ko edit karte hain, toh purana signature invalid ho jata hai.\n\nHamara app conversion ke dauran automatically APK me 'TestKey / Debug' v1 signature inject karta hai, jisse APK Android phone par bina error ke install ho sake!",
            englishBody = "Android OS strictly enforces digital signatures. Simply renaming a ZIP to APK results in an unsigned package that cannot be installed. This app automatically calculates SHA-256 digests and signs the repackaged APK with standard testkey certificates."
        )

        // Section 4: Website Alternatives vs This App
        GuideTopicCard(
            icon = Icons.Default.Language,
            iconColor = CyanPrimaryDark,
            title = "4. Websites vs Native App",
            hindiBody = "Agar aap PC browser par bina app ke convert karna chahte hain:\n• ezyZip (ezyzip.com/convert-apk-to-zip.html)\n• CloudConvert ya Online-Convert\n\nLekin websites par 100MB+ APKs upload karne me bahut data aur time lagta hai, aur privacy ka risk hota hai. Yeh app 100% OFFLINE aapke phone me hi instantly bina internet ke convert aur sign kar deta hai!",
            englishBody = "While web tools like ezyZip or CloudConvert exist for desktops, this native Android app operates completely offline with zero data consumption and no upload wait times."
        )

        // Section 5: Troubleshooting
        GuideTopicCard(
            icon = Icons.Default.Build,
            iconColor = MaterialTheme.colorScheme.secondary,
            title = "5. Common Errors & Solutions",
            hindiBody = "• 'There was a problem parsing the package': Yeh tab hota hai jab ZIP ke andar AndroidManifest.xml missing ho ya corrupt ho.\n• 'App not installed - Package conflicts': Agar phone me pehle se original Play Store version install hai, toh test-signed version install karne se pehle purana app uninstall karna padta hai.",
            englishBody = "If you encounter installation conflicts, ensure the existing Play Store version with different signatures is uninstalled before testing the repacked APK."
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GuideTopicCard(
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    hindiBody: String,
    englishBody: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("guide_card_${title.take(15)}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "हिन्दी (Hindi Guide)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = hindiBody,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "English Summary",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = englishBody,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
