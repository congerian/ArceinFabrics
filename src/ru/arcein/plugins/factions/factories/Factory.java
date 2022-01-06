package ru.arcein.plugins.factions.factories;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.integration.Econ;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Factory {

    //Статическая хуйня

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    //Параметры

    public String name; //Название фабрики

    public Location loc; //Центр платформы с фабрикой
    public double radius; //Сторона куба с центром в loc
    public double noHomeRange; //Расстояние, на котором нельзя ставить /f sethome

    public String start; //Начало сбора
    public String end; //Конец сбора
    public long secondsToWin; //Время захвата для победы

    public double reward; //Награда клану за победу

    public double radiusToAnnounce; //Радиус оповещений фабрики

    //Внутренние переменные

    boolean isActive; //Активна ли в данный момент

    boolean notified; //Выдавали ли оповещение

    public FactoriesManager manager;

    public Faction capturingFaction; //Захватывающая фракция.
    public Faction capturedFaction; //Захватившая фракция.
    public int progress; //Прогресс захвата в процентах.
    public Date lastCaptured; //Timestamp последнего захвата.

    public boolean isDeleted;

    public Hologram hologram;

    public boolean startIfTime(){
        if((dateToNextTime(start).getTime() - new Date().getTime())/1000L/60L == 15L && !this.notified){
            announceGlobalMessage(ChatColor.GREEN + "До старта фабрики " + name + " осталось 15 минут!");
            this.notified = true;
        }
        if(sdf.format(new Date()).equals(start) && !isActive){
            this.capturingFaction = Factions.getInstance().getWilderness();
            this.capturedFaction = Factions.getInstance().getWilderness();
            this.lastCaptured = new Date();
            this.isActive = true;
            this.notified = false;
            announceGlobalMessage(ChatColor.GREEN + "Фабрика " + name +
                    "(" + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + ") начала свою работу!");
            BukkitTask task = new Gathering(this).runTaskLater(this.manager.plugin, 1L);
            return true;
        }
        return false;
    }

    public Date dateToNextTime(String time){
        String[] times = time.split(":");
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(times[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(times[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if((cal.getTime().getTime() - date.getTime()) < 0)
        {
            cal.add(Calendar.DATE, 1);
        }
        date = cal.getTime();
        return date;
    }

    //TODO: Нормальная инициализация
    public Factory(FactoriesManager manager,
                   Location loc,
                   String start,
                   String end,
                   String name,
                   double radius,
                   double radiusToAnnounce,
                   double noHomeRange,
                   long secondsToWin,
                   double reward){

        this.manager = manager;
        this.isActive = false;
        this.notified = false;
        this.isDeleted = false;

        this.loc = loc;
        this.start = start;
        this.end = end;
        this.radius = radius;
        this.name = name;
        this.radiusToAnnounce = radiusToAnnounce;
        this.noHomeRange = noHomeRange;
        this.secondsToWin = secondsToWin;
        this.reward = reward;

        //Тут застартим HD
        hologram = HologramsAPI.createHologram(this.manager.plugin, this.loc.clone().add(0.0,4.0,0.0));

        new HandlingHologram(this).runTaskTimer(this.manager.plugin, 1L, 20L);
    }

    public void announceMessage(String msg){
        for(Player player : loc.getWorld().getPlayers()){
            if(player.getLocation().distance(loc) < radiusToAnnounce) player.sendMessage(msg);
        }
    }

    public void announceGlobalMessage(String msg){
        for(Player player : loc.getWorld().getPlayers()){
            player.sendMessage(msg);
        }
    }


    public class HandlingHologram extends BukkitRunnable{
        Factory factory;

        public HandlingHologram(Factory factory){
            this.factory = factory;
        }

        @Override
        public void run() {

            if(isDeleted){
                hologram.delete();
                this.cancel();
                return;
            }

            DecimalFormat df = new DecimalFormat("00");

            hologram.clearLines();
            hologram.appendTextLine("Фабрика: " + this.factory.name);

            if(factory.isActive){

                long secondsToEnd = (factory.dateToNextTime(this.factory.end).getTime() - new Date().getTime()) / 1000L;
                String timeToEnd = df.format((secondsToEnd / 60L)%60L) + ":"
                        + df.format(secondsToEnd % 60);

                hologram.appendTextLine("До конца работы фабрики осталось: " + timeToEnd);

                if(this.factory.capturedFaction.equals(Factions.getInstance().getWilderness())){
                    if(this.factory.capturingFaction.equals(Factions.getInstance().getWilderness())){
                        hologram.appendTextLine("Фабрика свободна!");
                    }
                    else{
                        hologram.appendTextLine("Фабрику захватывает: " + this.factory.capturingFaction.getTag());
                        hologram.appendTextLine("Прогресс: " + this.factory.progress + "%/100%");
                    }
                }
                else{
                    hologram.appendTextLine("Фабрика захвачена: " + this.factory.capturedFaction.getTag());
                    hologram.appendTextLine("Прогресс: " + this.factory.progress + "%/100%");
                    long secondsToWin = this.factory.secondsToWin - TimeUnit.SECONDS.convert (new Date().getTime()
                            - this.factory.lastCaptured.getTime(), TimeUnit.MILLISECONDS);
                    String timeToWin = df.format((secondsToWin / 60L)%60L) + ":"
                            + df.format(secondsToWin % 60);
                    hologram.appendTextLine("Победа через: " + timeToWin);
                }
            }

            else{
                long secondsToStart = (factory.dateToNextTime(this.factory.start).getTime() - new Date().getTime()) / 1000L;
                String time = df.format(secondsToStart / 3600L) + ":"
                        + df.format((secondsToStart / 60L)%60L) + ":"
                        + df.format(secondsToStart % 60);

                hologram.appendTextLine("Старт через: " + time);
            }
        }

    }

    public class Gathering extends BukkitRunnable {

        Factory factory;

        public Gathering(Factory factory){
            this.factory = factory;
        }

        @Override
        public void run() {

            //1. обрабатываем игроков в зоне и меняем прогрессбар

            Faction wild = Factions.getInstance().getWilderness();

            //1.1. Собираем игроков на платформе

            Map<Faction, List<Player>> playersInArea = new HashMap<>();

            for(Player player : loc.getWorld().getPlayers()){
                if(
                    player.getLocation().getX() < (loc.getX() + radius) &&
                    player.getLocation().getX() > (loc.getX() - radius) &&
                    player.getLocation().getY() < (loc.getY() + radius) &&
                    player.getLocation().getY() > (loc.getY() - radius) &&
                    player.getLocation().getZ() < (loc.getZ() + radius) &&
                    player.getLocation().getZ() > (loc.getZ() - radius)
                ) {
                    if(FPlayers.getInstance().getByPlayer(player).getFaction().equals(wild)){

                    }
                    else{
                        if(!playersInArea.containsKey(FPlayers.getInstance().getByPlayer(player).getFaction())){
                            playersInArea.put(FPlayers.getInstance().getByPlayer(player).getFaction(), new ArrayList<>());
                        }
                        playersInArea.get(FPlayers.getInstance().getByPlayer(player).getFaction()).add(player);

                    }
                }
            }

            //1.2. Отрабатываем логику

            //Если никого, то:
            if(playersInArea.isEmpty()){

                //Если захвачена кем-то, регенерируем:
                if(!factory.capturedFaction.equals(wild)){
                    factory.progress = Math.min(factory.progress + 1, 100);
                }
                //Если не захвачена никем, уменьшаем прогресс захвата:
                else {
                    factory.progress = Math.max(factory.progress - 5, 0);
                    //Если прогресс нулевой, сбрасываем захватывающую фракцию:
                    if(factory.progress == 0) {
                        if(!capturingFaction.equals(wild))announceMessage(ChatColor.YELLOW + "" + ChatColor.BOLD +
                                "Клан " + factory.capturingFaction.getTag() +
                                " больше не захватывает фабрику " + name + "!");
                        factory.capturingFaction = wild;
                    }
                }
            }
            //Если кто-то есть:
            else{
                //Если точка захвачена кем-то:
                if(!factory.capturedFaction.equals(wild)){
                    //Если на ней больше одного клана:
                    if(playersInArea.size() > 1){
                        //Если там есть захвативший клан:
                        if(playersInArea.containsKey(factory.capturedFaction)){
                            //Ничего не делаем
                        }
                        //Если его там нет - уменьшаем захват:
                        else{
                            //Считаем количество игроков:
                            int count = 0;
                            for(Map.Entry<Faction, List<Player>> entry : playersInArea.entrySet()) count += entry.getValue().size();
                            factory.progress = Math.max(factory.progress - count, 0);
                            //Если прогресс нулевой, сбрасываем захватившую фракцию:
                            if(factory.progress == 0) {
                                announceMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Фабрика " + name + " может быть захвачена вновь!");
                                factory.capturedFaction = wild;
                            }
                        }
                    }
                    //Если на ней один клан:
                    else {
                        Faction f = wild;
                        int count = 0;
                        for (Map.Entry<Faction, List<Player>> entry : playersInArea.entrySet()) {
                            f = entry.getKey();
                            count = entry.getValue().size();
                        }
                        //Если этот клан - захватившие:
                        if (f.equals(factory.capturedFaction)) {
                            factory.progress = Math.min(factory.progress + count + 5, 100);
                        }
                        //Если это другой клан:
                        else {
                            factory.progress = Math.max(factory.progress - count, 0);
                            //Если прогресс нулевой, сбрасываем захватившую фракцию:
                            if (factory.progress == 0) {
                                announceMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Фабрика " + name + " может быть захвачена вновь!");
                                factory.capturedFaction = wild;
                            }

                        }
                    }
                }
                //Точка никем не захвачена:
                else{
                    //Если в зоне больше 1 клана:
                    if(playersInArea.size() > 1){
                        //Если в зоне есть захватывающий клан:
                        if(playersInArea.containsKey(factory.capturingFaction)){
                            //Ничего не делаем, кто-то мешает захвату.
                        }
                        //В зоне нет захватывающего клана:
                        else{
                            //Считаем количество игроков, чтобы уменьшить прогресс:
                            int count = 0;
                            for(Map.Entry<Faction, List<Player>> entry : playersInArea.entrySet()) count += entry.getValue().size();
                            factory.progress = Math.max(factory.progress - (count + 5), 0);
                            //Если прогресс нулевой, сбрасываем захватившую фракцию:
                            if(factory.progress == 0) {
                                if(!capturingFaction.equals(wild))announceMessage(ChatColor.YELLOW + "" +
                                        ChatColor.BOLD + "Клан " + factory.capturingFaction.getTag() +
                                        " больше не захватывает фабрику " + name + "!");
                                factory.capturingFaction = wild;
                            }
                        }
                    }
                    //Если в зоне один клан:
                    else {
                        Faction f = wild;
                        int count = 0;
                        for(Map.Entry<Faction, List<Player>> entry : playersInArea.entrySet()){
                            f = entry.getKey();
                            count = entry.getValue().size();
                        }
                        //Если этот клан - захватывающие:
                        if(f.equals(factory.capturingFaction)) {
                            factory.progress = Math.min(factory.progress + count, 100);
                            //Если прогресс 100, то захватывающий клан становится захватившим:
                            if(factory.progress == 100) {
                                announceMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Клан " + factory.capturingFaction.getTag() +
                                        " захватил фабрику " + name + "!");
                                factory.capturedFaction = factory.capturingFaction;
                                factory.capturingFaction = wild;
                                factory.lastCaptured = new Date();
                            }
                        }
                        //Если это другой клан:
                        else{
                            //Если никто не захватывает:
                            if(factory.capturingFaction.equals(wild)){
                                //Делаем стоящий клан захватывающим:
                                factory.capturingFaction = f;
                                factory.progress = Math.min(factory.progress + count, 100);
                                announceMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Клан " + factory.capturingFaction.getTag() +
                                        " начал захватывать фабрику " + name + "!");
                            }
                            //Если захватывает другой клан, отличный от стоящих:
                            else {
                                //Уменьшаем прогресс:
                                factory.progress = Math.max(factory.progress - (count+5), 0);
                                //Если прогресс нулевой, сбрасываем захватывающую фракцию:
                                if(factory.progress == 0) {
                                    announceMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Клан " + factory.capturingFaction.getTag() +
                                            " больше не захватывает фабрику " + name + "!");
                                    factory.capturingFaction = wild;
                                }
                            }
                        }
                    }
                }
            }

            //2. если какая-то команда держит достаточно долго - конец:
            if(TimeUnit.SECONDS.convert (new Date().getTime()
                    - factory.lastCaptured.getTime(), TimeUnit.MILLISECONDS) > factory.secondsToWin && !factory.capturedFaction.equals(wild)){
                Econ.modifyMoney(factory.capturedFaction, factory.reward, "", "Вам начислено " + factory.reward + " за захват фабрики!");
                announceGlobalMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Клан " + factory.capturedFaction.getTag() +
                        " захватил фабрику " + name +
                        "(" + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + ") и получил свою заслуженную награду!");
                isActive = false;
                return;
            }

            //3. Если никто не выиграл - сворачиваем лавочку:
            if(sdf.format(new Date()).equals(end)) {
                isActive = false;
                announceGlobalMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Время доступности фабрики " +
                        name + "(" + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + ") вышло. Сегодня данная фабрика больше не принесёт прибыли!");
                return;
            }

            BukkitTask task = new Gathering(this.factory).runTaskLater(this.factory.manager.plugin, 20L);
        }
    }
}
