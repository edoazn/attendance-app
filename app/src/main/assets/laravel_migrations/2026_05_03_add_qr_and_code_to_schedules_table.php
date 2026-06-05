<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('schedules', function (Blueprint $table) {
            // Token unik untuk QR Code — di-generate otomatis saat schedule dibuat
            $table->string('qr_token')->nullable()->unique()->after('location_id');

            // Kode 6-karakter untuk input manual — di-generate otomatis saat schedule dibuat
            $table->string('attendance_code', 6)->nullable()->after('qr_token');
        });

        // Isi qr_token dan attendance_code untuk data schedule yang sudah ada
        \DB::table('schedules')->whereNull('qr_token')->get()->each(function ($schedule) {
            \DB::table('schedules')->where('id', $schedule->id)->update([
                'qr_token'        => (string) Str::uuid(),
                'attendance_code' => strtoupper(Str::random(6)),
            ]);
        });
    }

    public function down(): void
    {
        Schema::table('schedules', function (Blueprint $table) {
            $table->dropColumn(['qr_token', 'attendance_code']);
        });
    }
};
