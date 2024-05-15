import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import java.io._

object music {
  def main(args:Array[String]){
    val conf = new SparkConf().setAppName("MusicRecommendations")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder().getOrCreate()

    // 加载数据文件
    val df = spark.read.format("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "false")
      .option("delimiter", ",")
      .load("file:///home/hadoop/final/albums.csv")
    import spark.implicits._

    // 计算每种音乐类型的总数，并生成 JSON 文件
    val genreCount = df.groupBy("genre").count()
    val result1 = genreCount.toJSON.collectAsList.toString
    val writer1 = new PrintWriter(new File("/home/hadoop/final/re/result1.json"))
    writer1.write(result1)
    writer1.close()

    val genre_sales = df.select(df("genre"), df("num_of_sales")).rdd.map(v => (v(0).toString, v(1).toString.toInt)).reduceByKey(_+_).collect()
    val result2 = sc.parallelize(genre_sales).toDF().toJSON.collectAsList.toString
    val writer2 = new PrintWriter(new File("/home/hadoop/final/re/result2.json" ))
    writer2.write(result2)
    writer2.close()

    // 计算专辑总评分，并记录下总评分最高的 10 张专辑名及歌手 ID，并生成 JSON 文件
    val topAlbums = df.withColumn("total_score", $"rolling_stone_critic" * 0.4 + $"mtv_critic" * 0.4 + $"music_maniac_critic" * 0.2)
      .orderBy($"total_score".desc)
      .select($"album_title", $"artist_id")
      .limit(10)
    val result3 = topAlbums.toJSON.collectAsList.toString
    val writer3 = new PrintWriter(new File("/home/hadoop/final/re/result3.json"))
    writer3.write(result3)
    writer3.close()

    // 保持不变的 result4.json 文件

    val tmp = df.groupBy("genre").count()
    val genre_list = tmp.orderBy(tmp("count").desc).rdd.map(v=>v(0).toString).take(5)
    val genreYearHotArray = df.select(df("genre"), df("year_of_pub"), df("num_of_sales")).rdd.filter(v => genre_list.contains(v(0))).map(v => ((v(0).toString, v(1).toString.toInt), v(2).toString.toInt)).reduceByKey(_+_).sortBy(_._1._2).collect()
    val result4 = sc.parallelize(genreYearHotArray).toDF().toJSON.collectAsList.toString
    val writer4 = new PrintWriter(new File("/home/hadoop/final/re/result4.json" ))
    writer4.write(result4)
    writer4.close()
    // 关闭 SparkSession
    spark.close()
  }
}

