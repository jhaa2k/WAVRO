@Composable
fun SettingsScreen(vm: StudioViewModel) {
    val sleepTimer by vm.sleepTimerRemaining.collectAsState()
    val isExporting by vm.isExporting.collectAsState()
    val exportProgress by vm.exportProgress.collectAsState()
    val exportStatus by vm.exportStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // Sleep Timer Section...
        // (Keep existing Sleep Timer Code)
        
        Spacer(Modifier.height(32.dp))
        Divider(color = PN)
        Spacer(Modifier.height(16.dp))
        
        // --- EXPORT SECTION ---
        Text("Video Export (MP4)", color = TX, fontWeight = FontWeight.Bold)
        Text("Renders the visualizer at 1080x1920 (30fps).", color = DM, fontSize = 12.sp)
        
        Spacer(Modifier.height(8.dp))
        if (isExporting) {
            LinearProgressIndicator(progress = { exportProgress }, modifier = Modifier.fillMaxWidth(), color = AC)
            Text(exportStatus, color = AC, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            Button(
                onClick = { vm.exportVideo() },
                colors = ButtonDefaults.buttonColors(containerColor = AC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Export")
            }
            if (exportStatus.isNotEmpty()) {
                Text(exportStatus, color = DM, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}