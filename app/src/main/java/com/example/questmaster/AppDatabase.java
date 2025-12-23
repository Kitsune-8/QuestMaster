package com.example.questmaster;

import android.content.Context;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.room.Update;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "rooms")
class RoomEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String roomNumber;
    public int maxPlayers;
    public String genre;
    public double price;
    public double durationHours;
    public String description;
    public boolean isActive = true;
}

@Entity(tableName = "masters")
class MasterEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String fullName;
    public String phone;
    public String email;
    public double experienceYears;
    public String questsSpecialization;
    public String notes;
    public boolean isActive = true;
}

@Entity(tableName = "bookings",
        foreignKeys = {
                @ForeignKey(entity = RoomEntity.class,
                        parentColumns = "id",
                        childColumns = "roomId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = MasterEntity.class,
                        parentColumns = "id",
                        childColumns = "masterId",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index(value = {"roomId"}),
                @Index(value = {"masterId"}),
                @Index(value = {"bookingDate"}),
                @Index(value = {"startTime"})
        })
class BookingEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int roomId;
    public Integer masterId;
    public String clientName;
    public String clientPhone;
    public int playersCount;

    @TypeConverters({DateConverter.class})
    public Date bookingDate;

    public String startTime;
    public String endTime;
    public int startHour; // Добавлено для недельного календаря
    public int endHour;   // Добавлено для недельного календаря
    public double pricePerPerson;
    public double totalAmount;
    public double paidAmount;
    public String status = "ожидание";
    public String notes;

    @TypeConverters({DateConverter.class})
    public Date createdAt = new Date();
}

// TypeConverter
class DateConverter {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @TypeConverter
    public static Date fromString(String value) {
        if (value == null) return null;
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    @TypeConverter
    public static String dateToString(Date date) {
        if (date == null) return null;
        return format.format(date);
    }
}

// DAO интерфейс
@Dao
interface QuestMasterDao {
    // Комнаты
    @Insert
    long insertRoom(RoomEntity room);

    @Update
    void updateRoom(RoomEntity room);

    @Delete
    void deleteRoom(RoomEntity room);

    @Query("SELECT * FROM rooms ORDER BY name")
    List<RoomEntity> getAllRooms();

    @Query("SELECT * FROM rooms WHERE isActive = 1 ORDER BY name")
    List<RoomEntity> getActiveRooms();

    @Query("SELECT * FROM rooms WHERE id = :id")
    RoomEntity getRoomById(int id);

    // Мастера
    @Insert
    long insertMaster(MasterEntity master);

    @Update
    void updateMaster(MasterEntity master);

    @Delete
    void deleteMaster(MasterEntity master);

    @Query("SELECT * FROM masters ORDER BY fullName")
    List<MasterEntity> getAllMasters();

    @Query("SELECT * FROM masters WHERE isActive = 1 ORDER BY fullName")
    List<MasterEntity> getActiveMasters();

    @Query("SELECT * FROM masters WHERE id = :id")
    MasterEntity getMasterById(int id);

    // Бронирования
    @Insert
    long insertBooking(BookingEntity booking);

    @Update
    void updateBooking(BookingEntity booking);

    @Delete
    void deleteBooking(BookingEntity booking);

    @Query("SELECT * FROM bookings ORDER BY bookingDate DESC, startTime")
    List<BookingEntity> getAllBookings();

    @Query("SELECT * FROM bookings WHERE id = :id")
    BookingEntity getBookingById(int id);

    @Query("SELECT * FROM bookings WHERE bookingDate = :date AND status != 'отменена' ORDER BY startTime")
    List<BookingEntity> getBookingsByDate(Date date);

    @Query("SELECT * FROM bookings WHERE bookingDate BETWEEN :startDate AND :endDate AND status != 'отменена' ORDER BY bookingDate, startTime")
    List<BookingEntity> getBookingsByDateRange(Date startDate, Date endDate);

    @Query("SELECT * FROM bookings WHERE bookingDate BETWEEN :startDate AND :endDate AND roomId = :roomId AND status != 'отменена'")
    List<BookingEntity> getBookingsForRoomAndDateRange(Date startDate, Date endDate, int roomId);

    @Query("SELECT * FROM bookings WHERE bookingDate = :date AND roomId = :roomId AND status != 'отменена' AND " +
            "startTime < :endTime AND endTime > :startTime")
    List<BookingEntity> getOverlappingBookings(Date date, int roomId, String startTime, String endTime);

    @Query("SELECT SUM(totalAmount) FROM bookings WHERE bookingDate BETWEEN :startDate AND :endDate AND status = 'завершена'")
    Double getRevenue(Date startDate, Date endDate);

    @Query("SELECT COUNT(*) FROM bookings WHERE bookingDate BETWEEN :startDate AND :endDate AND status = 'завершена'")
    Integer getCompletedSessionsCount(Date startDate, Date endDate);
}

// Database класс
@Database(
        entities = {RoomEntity.class, MasterEntity.class, BookingEntity.class},
        version = 2, // Увеличиваем версию из-за добавления полей
        exportSchema = false
)
@TypeConverters({DateConverter.class})
abstract class AppDatabase extends RoomDatabase {
    abstract QuestMasterDao dao();

    private static volatile AppDatabase instance;

    static AppDatabase getDatabase(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "questmaster_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}