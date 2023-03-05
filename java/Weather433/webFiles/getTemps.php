<?php
// http://URL/getTemps.php/?time1=1534510800&time2=1534597200
// http://URL/getTemps.php?version

$version = "2.8";
if (isset($_GET["version"])) {
	echo nl2br("version=".$version.PHP_EOL);
} else {
	$beginTime = $_GET["time1"];   // equals epoch time -  dbStartTime
	$endTime = $_GET["time2"];

	foreach(file("hostDB.txt") as $line) {
		$tmpArray = explode('=', rtrim($line));
		if ($tmpArray[0] == "servername")
			$servername = $tmpArray[1];
		if ($tmpArray[0] == "username") 
			$username = $tmpArray[1];
		if ($tmpArray[0] == "password")
			$password = $tmpArray[1];
		if ($tmpArray[0] == "dbname") 
			$dbname = $tmpArray[1];
	}

	$results = array();
	
	try {
		$conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
		$conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);   
		$sql = "SELECT maxTime, maxTemp, minTime, minTemp FROM HourlyTemps where maxTime >= ".$beginTime." and maxTime < ".$endTime;
		$stmt = $conn->prepare($sql); 
		$stmt->execute();
		$stmt->setFetchMode(PDO::FETCH_NUM); 
		$result = $stmt->fetchAll();
		$indx = 0;
	
		foreach ($result as $key => $val) {       
			$results[$indx++] = $val[0]; // maxTime
			$results[$indx++] = $val[1]; // maxTemp
			$results[$indx++] = $val[2]; // minTime
			$results[$indx++] = $val[3]; // minTemp;
		}

		$myJSON = json_encode($results);
		echo $myJSON;    
	} catch(PDOException $e) {
		 echo nl2br("Error: " . $e->getMessage().PHP_EOL);
	}
}
?>