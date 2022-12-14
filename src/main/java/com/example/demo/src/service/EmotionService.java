package com.example.demo.src.service;

import com.example.demo.src.dto.response.BaseException;
import com.example.demo.src.dto.response.GetSearchEmotionRes;
import com.example.demo.src.dto.response.GetSearchTrackRes;
import com.example.demo.src.entity.Music;
import com.example.demo.src.entity.Post;
import com.example.demo.src.entity.User;
import com.example.demo.src.repository.MusicRepository;
import com.example.demo.src.repository.PostRepository;
import com.example.demo.src.repository.UserRepository;
import com.example.demo.src.spotify.SearchTrack;
import com.example.demo.src.spotify.SpotifyToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmotionService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MusicRepository musicRepository;

    private final SpotifyToken spotifyToken;

    private final MusicService musicService;

    private final SearchTrack searchTrack;


    private static String URL = "";
    private static final String GET = "GET";
    private static final String USER_AGENT = "Mozilla/5.0";

    @Autowired
    public EmotionService(PostRepository postRepository, UserRepository userRepository, MusicRepository musicRepository, SpotifyToken spotifyToken, MusicService musicService, SearchTrack searchTrack) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.musicRepository = musicRepository;
        this.spotifyToken = spotifyToken;
        this.musicService = musicService;
        this.searchTrack = searchTrack;
    }

    public List<GetSearchEmotionRes> searchRandomEmotion(Long userIdx) throws BaseException, IOException {
        List<GetSearchEmotionRes> result = new ArrayList<>();

        String emotions[] = {"HAPPY", "LOVELY", "ANGRY", "SAD", "EXPLODE", "TIRED"};
        // ?????? 6?????? ????????????
        for(int i = 0; i < 6; i++){
            User user = userRepository.findByUserIdx(userIdx);
            List<Post> postList = postRepository.findAllByUserAndEmotion(user, emotions[i]);
            if(postList.size() == 0) continue; // ????????? ?????????
            Collections.shuffle(postList); // ????????? ??? ?????? ?????? ??????

            // ?????? post??? ??????, ?????? ??????
            Music music = postList.get(0).getMusic();

            //?????? ??????????????? id ????????????
            String trackIdx=music.getTrackIdx();

            //?????? 4??? ??????

            List<String>response=musicService.searchLyrics(trackIdx);
            String lyrics="";
            for(int num=0;num<4;num++){
                lyrics+=response.get(num).concat("\n");
            }

            GetSearchEmotionRes getSearchEmotionRes = GetSearchEmotionRes.builder()
                    .postIdx(postList.get(0).getPostIdx())
                    .musicIdx(music.getMusicIdx())
                    .videoId(music.getVideoIdx())
                    .date(postList.get(0).getDate())
                    .artist(music.getArtist())
                    .track(music.getTrack())
                    .lyrics(lyrics)
                    .options(postList.get(0).getOptions())
                    .emotion(emotions[i])
                    .build();
            result.add((getSearchEmotionRes));
        }

        return result;
    }

    public List<GetSearchEmotionRes> searchForEmotion(Long userIdx, String emotion) throws BaseException, IOException {
        List<GetSearchEmotionRes> result = new ArrayList<>();

        User user = userRepository.findByUserIdx(userIdx);
        List<Post> postList = postRepository.findAllByUserAndEmotion(user, emotion);

        for(int i = 0; i < postList.size(); i++){
            // ?????? post??? ??????, ?????? ??????
            Music music = postList.get(i).getMusic();

            //?????? ??????????????? id ????????????
            String trackIdx=music.getTrackIdx();
            
            //?????? 4??? ??????

            List<String>response=musicService.searchLyrics(trackIdx);
            String lyrics="";
            for(int num=0;num<4;num++){
                lyrics+=response.get(num).concat("\n");
            }

            GetSearchEmotionRes getSearchEmotionRes = GetSearchEmotionRes.builder()
                    .postIdx(postList.get(i).getPostIdx())
                    .musicIdx(music.getMusicIdx())
                    .date(postList.get(i).getDate())
                    .artist(music.getArtist())
                    .track(music.getTrack())
                    .lyrics(lyrics)
                    .options(postList.get(i).getOptions())
                    .videoId(music.getVideoIdx())
                    .emotion(emotion)
                    .build();
            result.add((getSearchEmotionRes));
        }
        return result;
    }

    public List<List<GetSearchTrackRes>> searchForTrack(Long userIdx, String musicIdx) throws BaseException, IOException {
        List<List<GetSearchTrackRes>> result = new ArrayList<>();
        List<GetSearchTrackRes> moment = new ArrayList<>();
        List<GetSearchTrackRes> today = new ArrayList<>();

        User user = userRepository.findByUserIdx(userIdx);
        Music music = musicRepository.findByMusicIdx(Long.parseLong(musicIdx));

        //?????? ??????????????? id ????????????
        String trackIdx=music.getTrackIdx();

        //?????? 4??? ??????

        List<String>response=musicService.searchLyrics(trackIdx);
        String lyrics="";
        for(int num=0;num<4;num++){
            lyrics+=response.get(num).concat("\n");
        }
        List<Post> postList = postRepository.findAllByUserAndMusic(user, music);
        for(int i = 0; i < postList.size(); i++){
            // ?????? post??? ??????, ?????? ??????
            GetSearchTrackRes getSearchTrackRes = GetSearchTrackRes.builder()
                    .postIdx(postList.get(i).getPostIdx())
                    .musicIdx(music.getMusicIdx())
                    .date(postList.get(i).getDate())
                    .videoId(music.getVideoIdx())
                    .artist(music.getArtist())
                    .track(music.getTrack())
                    .lyrics(lyrics)
                    .emotion(postList.get(i).getEmotion())
                    .options(postList.get(i).getOptions())
                    .build();
            if(getSearchTrackRes.getOptions() == 0)
                moment.add(getSearchTrackRes);
            else if(getSearchTrackRes.getOptions() == 1)
                today.add(getSearchTrackRes);
        }
        result.add(moment);
        result.add(today);
        return result;
    }

    public List<GetSearchTrackRes> search(Long userIdx, String input) throws BaseException{
        List<GetSearchTrackRes> result = new ArrayList<>();

        User user = userRepository.findByUserIdx(userIdx); // ?????? ??????
        List<Post> userPostList = postRepository.findAllByUser(user); // ?????? user post ??????
        List<Music> musicList = musicRepository.findAllByArtistLikeOrTrackLike("%" + input + "%", "%" + input + "%"); // ??????, ???????????? ??????

        for(int i = 0; i < musicList.size(); i++){
            System.out.println("musicList = " + musicList.get(i).getMusicIdx());
        }

        // ?????? ??????id??? post??? ??????id ???????????? ??????
        for(int i = 0; i < musicList.size(); i++){
            for(int j = 0; j < userPostList.size(); j++){
                if(musicList.get(i).getMusicIdx() == userPostList.get(j).getMusic().getMusicIdx()){
                    GetSearchTrackRes getSearchTrackRes = GetSearchTrackRes.builder()
                            .postIdx(userPostList.get(j).getPostIdx())
                            .musicIdx(musicList.get(i).getMusicIdx())
                            .artist(musicList.get(i).getArtist())
                            .track(musicList.get(i).getTrack())
                            .emotion(userPostList.get(j).getEmotion())
                            .date(userPostList.get(j).getDate())
                            .options(userPostList.get(j).getOptions())
                            .build();
                    result.add(getSearchTrackRes);
                }
            }
        }

        return result;
    }
}
