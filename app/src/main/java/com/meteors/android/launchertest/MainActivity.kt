package com.meteors.android.launchertest

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meteors.android.launchertest.databinding.ActivityMainBinding
import com.meteors.android.launchertest.databinding.ItemRecyclerviewBinding
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.PACKAGE_USAGE_STATS
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_LOGS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.PACKAGE_USAGE_STATS,
                    Manifest.permission.READ_LOGS
                ),
                0
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ActivityAdapter(getAllActivities())

        /*
        val installedApplications =
            packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES)

        Log.d("LauncherMain", "size is ${installedApplications.size}")

         */

        queryUsageStatus()
        getBatteryCapacity()
        getLocation()
    }

    /**
     * @Description: 获取所有应用信息
     * @Param:
     * @return:
     * @Date: 2022/5/25
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("QueryPermissionsNeeded")
    private fun getAllActivities(): List<ResolveInfo> {
        val startupIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities =
            packageManager.queryIntentActivities(startupIntent, PackageManager.MATCH_ALL)
        Toast.makeText(this, "There are ${activities.size} activities", Toast.LENGTH_SHORT).show()
        activities.sortWith { a, b ->
            String.CASE_INSENSITIVE_ORDER.compare(
                a.loadLabel(packageManager).toString(),
                b.loadLabel(packageManager).toString()
            )
        }
        return activities
    }

    /**
     * @Description: 获取位置信息
     * @Param:
     * @return:
     * @Date: 2022/5/25
     */
    private fun getLocation() {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val location = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "没有位置权限", Toast.LENGTH_SHORT).show()
            return
        } else {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        location?.apply {
            binding.textLatitude.text = location.latitude.toString()
            binding.textLongitude.text = location.longitude.toString()
        }
    }

    /**
     * @Description: 获取电量
     * @Param:
     * @return:
     * @Date: 2022/5/25
     */
    private fun getBatteryCapacity() {
        val batteryManager: BatteryManager =
            getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        binding.textBattery.text =
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString()
    }

    /**
     * @Description: 获取应用使用状态
     * @Param:
     * @return:
     * @Date: 2022/5/25
     */
    private fun queryUsageStatus(): List<UsageStats> {
        val usageStatsManager: UsageStatsManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            } else {
                Log.d("Test", "系统版本过低")
                return emptyList()
            }
        val calendar = java.util.Calendar.getInstance()
        val hour: Int = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute: Int = calendar.get(java.util.Calendar.MINUTE)
        val second: Int = calendar.get(java.util.Calendar.SECOND)
        val startTimeMillis =
            System.currentTimeMillis() - (hour * 60 * 60 + minute * 60 + second) * 1000L
        Log.d("Test", "current time is $hour:$minute:$second")
        var usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTimeMillis,
            System.currentTimeMillis()
        )
        usageStats.sortWith { a, b ->
            (a.totalTimeInForeground - b.totalTimeInForeground).toInt()
        }
        usageStats = usageStats.filter {
            it.totalTimeInForeground != 0L
        }
        /*
        Log.d("Test", "${usageStats.size}")
        for (usageStat in usageStats) {
            Log.d(
                "Test",
                "${usageStat.packageName}, total time is ${usageStat.totalTimeInForeground.toDouble() / 60 / 60 / 60}"
            )
        }
         */
        return usageStats
    }

    @SuppressLint("SimpleDateFormat")
    private fun queryEvents(beginTime: Long, endTime: Long): List<UsageEvents.Event> {
        val usageStatsManager: UsageStatsManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            } else {
                Log.d("Test", "系统版本过低")
                return emptyList()
            }
        val simpleFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        val result = ArrayList<UsageEvents.Event>()
        while (events.hasNextEvent()){
            val ev:UsageEvents.Event = UsageEvents.Event()
            events.getNextEvent(ev)
            when(ev.eventType){
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (result.isEmpty()){
                        result.add(ev)
                    }else {
                        if (result[result.size - 1].packageName != ev.packageName){
                            result.add(ev)
                        }
                    }
                    Log.d("Test1", "EventType is ACTIVITY_RESUMED\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n"+
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    Log.d("Test1", "EventType is ACTIVITY_STOPPED\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n"+
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
                UsageEvents.Event.DEVICE_STARTUP -> {
                    Log.d("Test1", "EventType is DEVICE_STARTUP\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n"+
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    Log.d("Test1", "EventType is DEVICE_SHUTDOWN\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n"+
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    Log.d("Test1", "EventType is SCREEN_INTERACTIVE\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n" +
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    Log.d("Test1", "EventType is SCREEN_NON_INTERACTIVE\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n"+
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
                UsageEvents.Event.USER_INTERACTION -> {
                    Log.d("Test1", "EventType is USER_INTERACTION\n" +
                            " PackageName = ${ev.packageName}\n" +
                            "ClassName is  ${ev.className}\n"+
                            "time = ${simpleFormat.format(ev.timeStamp)}")
                }
            }
        }
        return result
    }

    @SuppressLint("SimpleDateFormat")
    private fun showEventInfo(){

        var packageInfo :PackageInfo
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5 * 60 * 1000L
        val events = queryEvents(beginTime, endTime)
        val simpleDateFormat = SimpleDateFormat()

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("5分钟内的应用使用情况")
        val eventsArray = Array(events.size, init = { "" })

        for (i in events.indices) {
            packageInfo = packageManager.getPackageInfo(events[i].packageName, PackageManager.GET_ACTIVITIES)
            val appName = packageInfo.applicationInfo.loadLabel(packageManager)
            eventsArray[i] = "应用名称：$appName\n" +
                    "启动时间：${simpleDateFormat.format(events[i].timeStamp)}"
        }

        dialogBuilder.setItems(
            eventsArray
        ) { _, _ -> }
        dialogBuilder.show()
    }

    /**
     * @Description: 展示应用使用信息
     * @Param:
     * @return:
     * @Date: 2022/5/25
     */
    private fun showApplicationInfo() {
        val usageStats = queryUsageStatus()
        if (usageStats.isEmpty()) {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("无权限")
            alertDialog.setMessage("请到设置->隐私->特殊权限设置中打开本应用的“使用情况访问权限")
            alertDialog.show()
            return
        }
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("应用使用情况")
        val usageStatsArray = Array(usageStats.size, init = { "" })
        for (i in usageStats.indices) {
            usageStatsArray[i] = "PackageName：${usageStats[i].packageName}\n" +
                    "前台总时间：${usageStats[i].totalTimeInForeground / 1000.0} seconds"
        }
        dialogBuilder.setItems(
            usageStatsArray
        ) { _, _ -> }
        dialogBuilder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.appInformation -> {
                showApplicationInfo()
                true
            }
            R.id.eventInformation -> {
                showEventInfo()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    }

    private class ActivityHolder(private val itemBinding: ItemRecyclerviewBinding) :
        RecyclerView.ViewHolder(itemBinding.root),
        View.OnClickListener {

        private lateinit var resolveInfo: ResolveInfo

        init {
            itemBinding.textApplicationName.setOnClickListener(this)
            itemBinding.imageIcon.setOnClickListener(this)
        }

        fun bindActivity(resolveInfo: ResolveInfo) {
            this.resolveInfo = resolveInfo
            val packageManager = itemView.context.packageManager
            val appName = resolveInfo.loadLabel(packageManager).toString()
            itemBinding.imageIcon.setImageDrawable(resolveInfo.loadIcon(packageManager))
            itemBinding.textApplicationName.text = appName
        }

        override fun onClick(v: View?) {
            val activityInfo = resolveInfo.activityInfo

            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(activityInfo.applicationInfo.packageName, activityInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            v?.context?.startActivity(intent)
        }
    }

    private class ActivityAdapter(val activities: List<ResolveInfo>) :
        RecyclerView.Adapter<ActivityHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityHolder {
            val itemBinding =
                ItemRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ActivityHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ActivityHolder, position: Int) {
            val resolveInfo = activities[position]
            holder.bindActivity(resolveInfo)
        }

        override fun getItemCount(): Int {
            return activities.size
        }

    }

}